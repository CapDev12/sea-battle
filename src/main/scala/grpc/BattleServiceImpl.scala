package grpc

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.Source
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.util.Timeout
import battle._
import model.Games.GameId
import model.Players.PlayerId
import actors.Manager._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BattleServiceImpl(actor: ActorRef[Message], implicit val timeout: Timeout)(implicit val scheduler: Scheduler) extends BattleService {

  def closePF(GameId: GameId): PartialFunction[Any, CompletionStrategy] = new PartialFunction[Any, CompletionStrategy] {
    def apply(x: Any): CompletionStrategy = CompletionStrategy.immediately

    def isDefinedAt(x: Any): Boolean = x match {
      case GameResultMsg(GameId, _) => true
      case _ => false
    }
  }

  def movesSrc(gameId: GameId, playerId: PlayerId): Source[ShotResult, NotUsed] = Source.actorRef[Message](
    completionMatcher = closePF(gameId),
    failureMatcher = PartialFunction.empty,
    bufferSize = 100,
    overflowStrategy = OverflowStrategy.dropHead
  )
    .collect {
      case ShotResultMsg(_, playerId, x, y, result) =>
        ShotResult(playerId.toString, x, y, result)
    }
    .mapMaterializedValue { act =>
      actor ! WatchMsg(gameId, playerId, act)
      NotUsed
    }

  override def start(in: Start): Future[StartResult] = {
    actor
      .ask[Result](act => CreateGameMsg(UUID.fromString(in.getPlayer1.id), UUID.fromString(in.getPlayer2.id), act))
      .collect {
        case CreateGameResultMsg(gameId, _, _, success, _) =>
          StartResult(success, gameId.toString)
      }
  }

  override def shot(in: Shot): Future[ShotResult] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)

//    actor
//      .ask[Result](act => ShotMsg(gameId, playerId, in.x, in.y, act))
//      .collect {
//        case ShotResultMsg(_, playerId, x, y, result) =>
//          ShotResult(playerId.toString, x, y, result)
//      }
    actor ! ShotMsg(gameId, playerId, in.x, in.y)
    Future.successful(ShotResult(playerId.toString, in.x, in.y, ""))
  }

  override def moves(in: Moves): Source[ShotResult, NotUsed] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)

    movesSrc(gameId, playerId)
  }

  override def setup(in: Setup): Future[SetupResult] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)
    val ships = in.ships.map { case Ship(x, y, direction, decks, _) =>
      val dir = if(direction.isVertical) true else false
      model.Ships.Ship(x, y, dir, decks.toByte)
    }

    actor
      .ask[Result](act => SetupGameMsg(gameId, playerId, ships, act))
      .collect {
        case SetupGameResultMsg(gameId, playerId, success, message) =>
          SetupResult(gameId.toString, playerId.toString, success, message.getOrElse(""))
      }
  }

}
