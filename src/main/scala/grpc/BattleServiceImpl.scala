package grpc

import actors.Manager
import actors.Manager.{CreateGame, GameClose, ManagerEvent, SetupGame, SetupGameResult, ShotCmd, ShotResultEvent, StatusEvent, StatusResultEvent, WatchShots}
import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import battle._
import model.GameStatus.gameStatusToStr
import model.Players.{GameId, PlayerId}
import model.Ships.{Horizontal, Vertical}
import model.Shots.shotResultToStr

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class BattleServiceImpl(actor: ActorRef[Manager.ManagerEvent])(implicit val scheduler: Scheduler) extends BattleService {

  implicit val timeout: Timeout = 1.seconds

  def closePF(GameId: GameId): PartialFunction[Any, CompletionStrategy] = new PartialFunction[Any, CompletionStrategy] {
    def apply(x: Any): CompletionStrategy = CompletionStrategy.immediately
    def isDefinedAt(x: Any): Boolean = x match {
      case GameClose(_, _) => true
      case _ => false
    }
  }

  def movesActor(gameId: GameId, playerId: PlayerId): Source[ShotResult, NotUsed] = Source.actorRef[ManagerEvent](
    completionMatcher = closePF(gameId),
    failureMatcher = PartialFunction.empty,
    bufferSize = 100,
    overflowStrategy = OverflowStrategy.dropHead
  )
    .collect {
      case ShotResultEvent(_, playerId, x, y, result) => ShotResult(playerId.toString, x, y, shotResultToStr(result))
    }
    .mapMaterializedValue { act =>
      actor ! WatchShots(gameId, playerId, act)
      NotUsed
    }


  override def start(in: Start): Future[StartResult] = {
    actor
      .ask[ManagerEvent](act => CreateGame(UUID.fromString(in.getPlayer1.id), UUID.fromString(in.getPlayer2.id), act))
      .collect {
        case Manager.CreateGameResult(gameId, playerId1, playerId2, success, replyTo) =>
          StartResult(success, gameId.toString)
      }
  }

  override def shot(in: Shot): Future[ShotResult] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)

    actor
      .ask[ManagerEvent](act => ShotCmd(gameId, playerId, in.x, in.y, act))
      .collect {
        case ShotResultEvent(gameId, playerId, x, y, result) =>
          ShotResult(playerId.toString, x, y, shotResultToStr(result))
      }
  }

  override def status(in: Status): Future[StatusResult] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)

    actor
      .ask[ManagerEvent](act => StatusEvent(gameId, playerId, act))
      .collect {
        case StatusResultEvent(gameId, playerId, status) =>
          StatusResult(gameId.toString, playerId.toString, gameStatusToStr(status))
      }
  }

  override def moves(in: Moves): Source[ShotResult, NotUsed] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)

    movesActor(gameId, playerId)
  }

  override def setup(in: Setup): Future[SetupResult] = {
    val gameId = UUID.fromString(in.gameId)
    val playerId = UUID.fromString(in.playerId)
    val ships = in.ships.map { case Ship(x, y, direction, decks, _) =>
      val dir = if(direction.isVertical) true else false
      model.Ships.Ship(x, y, dir, decks.toByte)
    }

    actor
      .ask[ManagerEvent](act => SetupGame(gameId, playerId, ships, act))
      .collect {
        case SetupGameResult(gameId, playerId, success) =>
          SetupResult(gameId.toString, playerId.toString, success)
      }
  }

}
