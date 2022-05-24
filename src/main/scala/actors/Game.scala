package actors

import actors.Manager._
import akka.actor
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, Recovery}
import model.GameStatus.GameStatus
import model.Players.{GameId, PlayerData, PlayerId, PlayersData, initData}
import model.Ships._
import model.Shots._
import model.{GameStatus, Shots}
import utils.BattleMath.hit
import org.slf4j.Logger

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Game {

  val setupTimeout: FiniteDuration = 10.seconds
  val moveTimeout: FiniteDuration = 5.seconds

  sealed trait State extends CborSerializable
  case class Init() extends State
  case class Setup(gameId: GameId, playerId1: PlayerId, playerId2: PlayerId, data: PlayersData, managerRef: ActorRef[ManagerEvent], movesAct: Seq[akka.actor.ActorRef]) extends State
  case class Battle(gameId: GameId, moverId: PlayerId, data: PlayersData, movesAct: Seq[akka.actor.ActorRef]) extends State
  case class EndGame(gameId: GameId, winnerId: Option[PlayerId]) extends State

  sealed trait Command extends CborSerializable
  case class CreateGameCmd(gameId: GameId, playerId1: PlayerId, playerId2: PlayerId, replyTo: ActorRef[Manager.ManagerEvent], managerRef: ActorRef[ManagerEvent]) extends Command
  case class SetupGameCmd(playerId: PlayerId, ships: Seq[Ship], replyTo: ActorRef[Manager.ManagerEvent]) extends Command
  case object SetupTimeoutCmd extends Command
  case class ShotCmd(playerId: PlayerId, x: Int, y: Int, replyTo: ActorRef[Manager.ManagerEvent], managerRef: ActorRef[Manager.ManagerEvent]) extends Command
  case class MoveTimeoutCmd(playerId: PlayerId) extends Command
  case class WatchCmd(actor: akka.actor.ActorRef) extends Command

  sealed trait Event extends CborSerializable
  case class CreateGameEvent(gameId: GameId, playerId1: PlayerId, playerId2: PlayerId, managerRef: ActorRef[Manager.ManagerEvent]) extends Event
  case class SetupGameEvent(gameId: GameId, playerId: PlayerId, ships: Seq[Ship]) extends Event
  case object SetupTimeoutEvent extends Event
  case class ShotEvent(playerId: PlayerId, x: Int, y: Int) extends Event
  case object MoveTimeoutEvent extends Event
  case class WatchEvent(actor: akka.actor.ActorRef) extends Event

  def apply(entityId: String): Behavior[Command] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        EventSourcedBehavior[Command, Event, State](
          persistenceId = PersistenceId("Game", entityId),
          emptyState = Init(),
          commandHandler = commandHandler(timers, context.log),
          eventHandler = eventHandler(context.log)
        )
          .receiveSignal { case (state, RecoveryCompleted) =>
            state match {
              case _: Init =>

              case _: Setup =>
                timers.startSingleTimer(SetupTimeoutCmd, SetupTimeoutCmd, setupTimeout)

              case Battle(_, moverId, _, _) =>
                timers.startSingleTimer(MoveTimeoutCmd(moverId), MoveTimeoutCmd(moverId), moveTimeout)

              case _: EndGame =>
            }
          }
//          .withRecovery(Recovery.disabled)
      )
    )

  private def commandHandler(timers: TimerScheduler[Command], log: Logger): (State, Command) => Effect[Event, State] = { (state, command) =>
    //log.info(s"state: $state cmd: $command")
    state match {
      case Init() =>
        command match {
         case CreateGameCmd(gameId, playerId1, playerId2, replyTo: ActorRef[Manager.ManagerEvent], managerRef: ActorRef[ManagerEvent]) =>
           createCmd(gameId, playerId1, playerId2, replyTo, managerRef, timers, log)

         case _ =>
           Effect.none
        }

      case Setup(gameId, playerId1, palyerId2, data, managerRef, movesAct) =>
        command match {
          case SetupGameCmd(playerId, ships, replyTo) =>
            setupCmd(gameId, data, playerId, ships, replyTo, timers, log)

          case SetupTimeoutCmd =>
            setupTimeoutCmd(gameId, managerRef, movesAct)

          case WatchCmd(actor) =>
            watchCmd(actor)

          case _ =>
            Effect.none
        }

      case Battle(gameId, moverId, data, movesAct) =>
        command match {
          case cmd: ShotCmd =>
            shotCmd(cmd, data, gameId, moverId, timers, movesAct, log)

          case MoveTimeoutCmd(playerId) =>
            moveTimeoutCmd(gameId, moverId, data, timers, movesAct, log)

          case _ =>
            Effect.none
        }

      case EndGame(gameId, winnerId) =>
        command match {
          case _ =>
            Effect.none
        }

    }
  }

  private def createCmd(gameId: GameId, playerId1: PlayerId, playerId2: PlayerId, replyTo: ActorRef[Manager.ManagerEvent], managerRef: ActorRef[ManagerEvent], timers: TimerScheduler[Command], log: Logger): Effect[Event, State] = {
    if(playerId1 != playerId2)
      Effect
        .persist(CreateGameEvent(gameId, playerId1, playerId2, managerRef))
        .thenRun { state =>
          managerRef ! CreateGameResult(gameId, playerId1, playerId2, success = true, replyTo)
          timers.startSingleTimer(SetupTimeoutCmd, SetupTimeoutCmd, setupTimeout)
          log.info(s"Game created gameId: $gameId, playerId1: $playerId1, playerId2: $playerId2")
        }
    else {
      log.info(s"Game can't create playerId1 == playerId2. GameId: $gameId, playerId1: $playerId1, playerId2: $playerId2")
      Effect.none
    }
  }

  private def setupCmd(gameId: GameId, data: PlayersData, playerId: PlayerId, ships: Seq[Ship], replyTo: ActorRef[Manager.ManagerEvent], timers: TimerScheduler[Command], log: Logger): Effect[Event, State] = {
    if (!data.contains(playerId)) {
      log.info(s"Trying to setup ships for a non-existent player. gameId: $gameId, playerId: $playerId")
      Effect
        .none
        .thenRun(_ => replyTo ! SetupGameResult(gameId, playerId, success = false))
    } else if (!checkShips(ships)) {
      log.info(s"Trying to setup non-valid ships. gameId: $gameId, playerId: $playerId")
      Effect
        .none
        .thenRun(_ => replyTo ! SetupGameResult(gameId, playerId, success = false))
    } else {
      Effect
        .persist(SetupGameEvent(gameId, playerId, ships))
        .thenRun {
          case state: Setup =>
            log.info(s"Game setup. GameId: $gameId, playerId: $playerId")
            replyTo ! SetupGameResult(gameId, playerId, success = true)

          case state: Battle =>
            log.info(s"Game setup complete. GameId: $gameId, playerId: $playerId")
            replyTo ! SetupGameResult(gameId, playerId, success = true)
            timers.cancel(SetupTimeoutCmd)
            val firstMoverId = nextPlayerId(playerId, data.keys.toSeq)
            timers.startSingleTimer(MoveTimeoutCmd(firstMoverId), MoveTimeoutCmd(firstMoverId), moveTimeout)
        }
    }
  }

  private def setupTimeoutCmd(gameId: GameId, managerRef: ActorRef[Manager.ManagerEvent], movesAct: Seq[akka.actor.ActorRef]): Effect[Event, State] = {
    Effect
      .persist(SetupTimeoutEvent)
      .thenRun { _ =>
        val msg = GameClose(gameId, None)
        managerRef ! msg
        movesAct.foreach(_ ! msg)
      }
  }

  private def shotCmd(cmd: ShotCmd, data: PlayersData, gameId: GameId, moverId: PlayerId, timers: TimerScheduler[Command], movesAct: Seq[akka.actor.ActorRef], log: Logger): Effect[Event, State] = {
    if (cmd.playerId == moverId) {
      timers.cancel(MoveTimeoutCmd(moverId))

      val event = ShotEvent(cmd.playerId, cmd.x, cmd.y)

      Effect
        .persist(event)
        .thenRun { state =>
          val targetPlayerId = nextPlayerId(cmd.playerId, data.keys.toSeq)

          val targetShips = data(targetPlayerId).ships
          val moverShots = data(cmd.playerId).shots
          val shotResultEvent = hit(targetShips, moverShots.toSet, Shot(cmd.x, cmd.y))

          cmd.replyTo ! ShotResultEvent(gameId, cmd.playerId, cmd.x, cmd.y, shotResultEvent)
          movesAct.foreach(_ ! ShotResultEvent(gameId, cmd.playerId, cmd.x, cmd.y, shotResultEvent))
          log.info(s"Shot gameId: $gameId playerId: ${cmd.playerId} x: ${cmd.x} y: ${cmd.y} result: $shotResultEvent")

          if (shotResultEvent == Shots.Won) {
            val msg = GameClose(gameId, Some(moverId))
            cmd.managerRef ! msg
            movesAct.foreach(_ ! msg)
          } else
            timers.startSingleTimer(MoveTimeoutCmd(targetPlayerId), MoveTimeoutCmd(targetPlayerId), moveTimeout)
        }
    } else {
      Effect
        .none
        .thenRun { state =>
          cmd.replyTo ! ShotResultEvent(gameId, cmd.playerId, cmd.x, cmd.y, Shots.NotYourTurn)
          movesAct.foreach(_ ! ShotResultEvent(gameId, cmd.playerId, cmd.x, cmd.y, Shots.NotYourTurn))
          log.info(s"Shot gameId: $gameId playerId: ${cmd.playerId} x: ${cmd.x} y: ${cmd.y} result: NotYourTurn")
        }
    }
  }

  private def moveTimeoutCmd(gameId: GameId, moverId: PlayerId, data: PlayersData, timers: TimerScheduler[Command], movesAct: Seq[akka.actor.ActorRef], log: Logger): Effect[Event, State] = {
    Effect
      .persist(MoveTimeoutEvent)
      .thenRun { state =>
        movesAct.foreach(_ ! ShotResultEvent(gameId, moverId, 0, 0, Shots.Timeout))
        val nextMoverId = nextPlayerId(moverId, data.keys.toSeq)
        timers.startSingleTimer(MoveTimeoutCmd(nextMoverId), MoveTimeoutCmd(nextMoverId), moveTimeout)
      }
  }

  private def watchCmd(act: actor.ActorRef): Effect[Event, State] = {
    Effect
      .persist(WatchEvent(act))
  }


  private def eventHandler(log: Logger): (State, Event) => State = { (state, event) => {
    //log.info(s"state: $state evt: $event")
    state match {
      case Init() =>
        event match {
          case CreateGameEvent(gameId, playerId1, playerId2, managerRef) =>
            val data = initData(playerId1, playerId2)
            Setup(gameId, playerId1, playerId2, data, managerRef, Seq())
        }

      case Setup(gameId, playerId1, playerId2, data, managerRef, movesAct) =>
        event match {
          case SetupGameEvent(gameId, playerId, ships) =>
            val playerData = data(playerId).copy(ships = ships, setupShips = true)
            val dataWithShips = data + (playerId -> playerData)

            if (completeShipSetup(dataWithShips))
              Battle(gameId, playerId1, dataWithShips, movesAct)
            else
              Setup(gameId, playerId1, playerId2, dataWithShips, managerRef, movesAct)

          case SetupTimeoutEvent =>
            EndGame(gameId, None)

          case WatchEvent(actor) =>
            Setup(gameId, playerId1, playerId2, data, managerRef, movesAct :+ actor)
        }

      case Battle(gameId, moverId, data, movesAct) =>
        event match {
          case ShotEvent(playerId, x, y) =>
            val targetPlayerId = nextPlayerId(playerId, data.keys.toSeq)

            val targetShips = data(targetPlayerId).ships
            val moverShots = data(playerId).shots
            val shotResultEvent = hit(targetShips, moverShots.toSet, Shot(x, y))

            if (shotResultEvent == Shots.Won)
              EndGame(gameId, Some(playerId))
            else {
              val dataWithShot = makeShot(playerId, Shot(x, y), data)
              Battle(gameId, targetPlayerId, dataWithShot, movesAct)
            }

          case MoveTimeoutEvent =>
            Battle(gameId, nextPlayerId(moverId, data.keys.toSeq), data, movesAct)
        }

    }
  }}

  private def makeShot(playerId: PlayerId, shot: Shot, data: PlayersData): PlayersData = {
    val playerData = data(playerId).copy(shots = data(playerId).shots :+ shot)
    data + (playerId -> playerData)
  }

  private def nextPlayerId(playerId: PlayerId, playersIds: Seq[PlayerId]): PlayerId =
    playersIds.find(_ != playerId).get

  private def checkShips(ships: Seq[Ship]): Boolean =
    checkShipsDecks(ships) && checkShipsBounds(ships)

  private def completeShipSetup(data: PlayersData): Boolean =
    data.count(_._2.setupShips) == 2

}
