package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import model.Games.GameId
import model.Players.PlayerId
import model.Ships.Ship
import model.Shots
import org.slf4j.Logger
import utils.Utils

object Manager {

  sealed trait Message extends CborSerializable
  sealed trait Result extends CborSerializable

  case class CreateGameMsg(playerId1: PlayerId, playerId2: PlayerId, replyTo: ActorRef[Result]) extends Message
  case class CreateGameResultMsg(gameId: GameId, playerId1: PlayerId, playerId2: PlayerId, success: Boolean,
                                 replyTo: ActorRef[Result]) extends Message with Result

  case class SetupGameMsg(gameId: GameId, playerId: PlayerId, ships: Seq[Ship], replyTo: ActorRef[Result]) extends Message
  case class SetupGameResultMsg(gameId: GameId, playerId: PlayerId, success: Boolean, message: Option[String] = None) extends Message with Result

  case class GameResultMsg(gameId: GameId, winnerId: Option[PlayerId]) extends Message with Result

  case class ShotMsg(gameId: GameId, playerId: PlayerId, x: Int, y: Int/*, replyTo: ActorRef[Result]*/) extends Message
  case class ShotResultMsg(gameId: GameId, playerId: PlayerId, x: Int, y: Int, result: String) extends Message with Result

  case class WatchMsg(gameId: GameId, playerId: PlayerId, actorRef: akka.actor.ActorRef) extends Message

  def apply(sharding: GameSharding): Behavior[Message] =
    gameBehavior(0L, Set())(sharding)

  private def gameBehavior(gameCount: Long, games: Set[GameId])(implicit sharding: GameSharding): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(
        gamePF(context.log, gameCount, games, context.self.unsafeUpcast) orElse
        shotPF(context.log, games, context.self.unsafeUpcast) orElse
        watchPF
      )
    }

  private def gamePF(log: Logger, gameCount: Long, games: Set[GameId], managerRef: ActorRef[Result])
                    (implicit sharding: GameSharding): PartialFunction[Message, Behavior[Message]] = {
    case CreateGameMsg(playerId1, playerId2, replyTo) =>
      val gameId = Utils.uuid
      val game = sharding.entityRefFor(gameId.toString)

      game ! Game.CreateGameCmd(gameId, playerId1, playerId2, replyTo, managerRef)

      gameBehavior(gameCount + 1, games)

    case createGameResult @ CreateGameResultMsg(gameId, playerId1, playerId2, success, replyTo) =>
      log.info(s"Game started gameId: $gameId playerId1: $playerId1, playerId2: $playerId2, " +
        s"success: $success  gameCount: ${games.size + 1}")
      replyTo ! createGameResult
      gameBehavior(gameCount, games + gameId)

    case SetupGameMsg(gameId, playerId, ships, replyTo) =>
      if(games.contains(gameId)) {
        log.info(s"Setup gameId: $gameId playerId: $playerId ships: $ships")
        val game = sharding.entityRefFor(gameId.toString)
        game ! Game.SetupGameCmd(playerId, ships, replyTo)
      } else {
        log.info(s"Setup gameId: $gameId not found")
        replyTo ! SetupGameResultMsg(gameId, playerId, success = false)
      }
      Behaviors.same

    case GameResultMsg(gameId, winnerId) =>
      log.info(s"Close gameId: $gameId winnerId: $winnerId gameCount: ${games.size - 1}")
      gameBehavior(gameCount, games - gameId)
  }

  private def shotPF(log: Logger, games: Set[GameId], managerRef: ActorRef[Result])
                    (implicit sharding: GameSharding): PartialFunction[Message, Behavior[Message]] = {
    case ShotMsg(gameId, playerId, x, y/*, replyTo*/) =>
      if(games.contains(gameId)) {
        val game = sharding.entityRefFor(gameId.toString)
        game ! Game.ShotCmd(playerId, x, y, /*replyTo,*/ managerRef)
      } else {
        log.info(s"Shot gameId: $gameId not found")
        //replyTo ! ShotResultMsg(gameId, playerId, x, y, Shots.GameNotFound.toString)
      }

      Behaviors.same
  }

  private def watchPF(implicit sharding: GameSharding): PartialFunction[Message, Behavior[Message]] = {
    case WatchMsg(gameId, _, actorRef) =>
      val game = sharding.entityRefFor(gameId.toString)
      game ! Game.WatchCmd(actorRef)
      Behaviors.same
  }

}
