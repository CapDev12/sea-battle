package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import model.GameStatus.GameStatus
import model.Players.{GameId, PlayerId}
import model.Ships.Ship
import model.Shots.ShotResult
import model.{GameStatus, Shots}
import org.slf4j.Logger
import utils.Utils

object Manager {

  sealed trait ManagerEvent extends CborSerializable

  case class CreateGame(playerId1: PlayerId, playerId2: PlayerId, replyTo: ActorRef[ManagerEvent]) extends ManagerEvent
  case class CreateGameResult(gameId: GameId, playerId1: PlayerId, playerId2: PlayerId, success: Boolean, replyTo: ActorRef[ManagerEvent]) extends ManagerEvent

  case class SetupGame(gameId: GameId, playerId: PlayerId, ships: Seq[Ship], replyTo: ActorRef[Manager.ManagerEvent]) extends ManagerEvent
  case class SetupGameResult(gameId: GameId, playerId: PlayerId, success: Boolean) extends ManagerEvent

  case class GameClose(gameId: GameId, winnerId: Option[PlayerId]) extends ManagerEvent

  case class ShotCmd(gameId: GameId, playerId: PlayerId, x: Int, y: Int, replyTo: ActorRef[Manager.ManagerEvent]) extends ManagerEvent
  case class ShotResultEvent(gameId: GameId, playerId: PlayerId, x: Int, y: Int, result: ShotResult) extends ManagerEvent

  case class StatusEvent(gameId: GameId, playerId: PlayerId, replyTo: ActorRef[Manager.ManagerEvent]) extends ManagerEvent
  case class StatusResultEvent(gameId: GameId, playerId: PlayerId, status: GameStatus) extends ManagerEvent

  case class WatchShots(gameId: GameId, playerId: PlayerId, actor: akka.actor.ActorRef) extends ManagerEvent

  val TypeKey: EntityTypeKey[Game.Command] = EntityTypeKey[Game.Command]("Game")

  def apply(): Behavior[ManagerEvent] = Behaviors.setup { context =>
    implicit val sharding: ClusterSharding = ClusterSharding(context.system)

    sharding.init(Entity(TypeKey)(createBehavior = entityContext => Game(entityContext.entityId)))
    context.spawn(ClusterListener(), "ClusterListener")

    gameBehavior(0L, Set())
  }

  private def gameBehavior(gameCount: Long, games: Set[GameId])(implicit sharding: ClusterSharding): Behavior[ManagerEvent] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial(
        gamePF(context.log, gameCount, games, context.self) orElse
          shotPF(context.log, games, context.self) orElse
          //statusPF(games) orElse
          watchingPF
      )
    }

  private def gamePF(log: Logger, gameCount: Long, games: Set[GameId], managerRef: ActorRef[ManagerEvent])(implicit sharding: ClusterSharding): PartialFunction[ManagerEvent, Behavior[ManagerEvent]] = {
    case CreateGame(playerId1, playerId2, replyTo) =>
      val gameId = Utils.uuid
      val game = sharding.entityRefFor(TypeKey, gameId.toString)

      game ! Game.CreateGameCmd(gameId, playerId1, playerId2, replyTo, managerRef)

      gameBehavior(gameCount + 1, games)

    case createGameResult @ CreateGameResult(gameId, playerId1, playerId2, success, replyTo) =>
      log.info(s"Game started gameId: $gameId playerId1: $playerId1, playerId2: $playerId2, success: $success  gamesCount: ${games.size + 1}")
      replyTo ! createGameResult
      gameBehavior(gameCount, games + gameId)

    case SetupGame(gameId, playerId, ships, replyTo) =>
      if(games.contains(gameId)) {
        log.info(s"Setup gameId: $gameId playerId: $playerId ships: $ships")
        val game = sharding.entityRefFor(TypeKey, gameId.toString)
        game ! Game.SetupGameCmd(playerId, ships, replyTo)
      } else {
        log.info(s"Setup gameId: $gameId not found")
        replyTo ! SetupGameResult(gameId, playerId, success = false)
      }
      Behaviors.same

    case GameClose(gameId, winnerId) =>
      log.info(s"Close gameId: $gameId winnerId: $winnerId gameCount: ${games.size - 1}")
      gameBehavior(gameCount, games - gameId)
  }

  private def shotPF(log: Logger, games: Set[GameId], managerRef: ActorRef[ManagerEvent])(implicit sharding: ClusterSharding): PartialFunction[ManagerEvent, Behavior[ManagerEvent]] = {
    case ShotCmd(gameId, playerId, x, y, replyTo) =>
      if(games.contains(gameId)) {
        val game = sharding.entityRefFor(TypeKey, gameId.toString)
        game ! Game.ShotCmd(playerId, x, y, replyTo, managerRef)
      } else {
        log.info(s"Shot gameId: $gameId not found")
        replyTo ! ShotResultEvent(gameId, playerId, x, y, Shots.GameNotFound)
      }

      Behaviors.same
  }

  private def watchingPF(implicit sharding: ClusterSharding): PartialFunction[ManagerEvent, Behavior[ManagerEvent]] = {
    case WatchShots(gameId, playerId, actor) =>
      val game = sharding.entityRefFor(TypeKey, gameId.toString)
      game ! Game.WatchCmd(actor)
      Behaviors.same
  }

//  private def statusPF(games: Set[GameId])(implicit sharding: ClusterSharding): PartialFunction[ManagerEvent, Behavior[ManagerEvent]] = {
//    case StatusEvent(gameId, playerId, replyTo) =>
//      if(games.contains(gameId)) {
//        val game = sharding.entityRefFor(TypeKey, gameId.toString)
//        game ! Game.StatusEvent(playerId, replyTo)
//      } else
//        replyTo ! StatusResultEvent(gameId, playerId, GameStatus.NotFound)
//
//      Behaviors.same
//  }

}
