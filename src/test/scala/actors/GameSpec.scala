package actors

import actors.Game._
import actors.Manager.{CreateGameResultMsg, GameResultMsg, SetupGameResultMsg, ShotResultMsg}
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import model.Games.GameId
import model.Players.{PlayerData, PlayerId}
import model.Rules
import model.Rules.{ShipRule, ShipRules}
import model.Ships.Ship
import model.Shots._
import org.scalatest.wordspec.AnyWordSpecLike
import utils.Utils.uuid

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class GameSpec extends
  ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load()))
  with AnyWordSpecLike {

  private val shipRules: ShipRules = Set(
    ShipRule(2, 1),
    ShipRule(1, 1)
  )

  private val gameId = uuid
  private val playerId1 = uuid
  private val playerId2 = uuid

  private val probe = TestProbe[Manager.Result]()

  private val ships = Seq(
    Ship(x = 2, y = 2, dir = true, decks = 2),
    Ship(x = 4, y = 2, dir = false, decks = 1)
  )

  "Game" must {

    //Check life cycle with events and inner state
    "follow the life cycle" in {

      implicit val eventSourcedTestKit: EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State] =
        EventSourcedBehaviorTestKit(system, Game("entityId1", 10.seconds, 5.seconds, Rules.fieldWidth, Rules.fieldHeight, shipRules))

      val shotCmd11 = ShotCmd(playerId1, ships(0).x, ships(0).y, probe.ref, probe.ref)      //Injured
      val shotCmd21 = ShotCmd(playerId2, 1, 1, probe.ref, probe.ref)                        //Missed
      val shotCmd12 = ShotCmd(playerId1, ships(0).x, ships(0).y + 1, probe.ref, probe.ref)  //Destroyed
      val shotCmd22 = ShotCmd(playerId2, 3, 3, probe.ref, probe.ref)                        //Missed
      val shotCmd13 = ShotCmd(playerId1, ships(1).x, ships(1).y, probe.ref, probe.ref)      //Won

      val setupState = Setup(
        gameId = gameId,
        playerId1 = playerId1,
        playerId2 = playerId2,
        data = Map(
          playerId1 -> PlayerData(List(), List()),
          playerId2 -> PlayerData(List(), List())
        ),
        managerRef = probe.ref,
        movesAct = List()
      )

      execCmd(
        cmd = CreateGameCmd(gameId, playerId1, playerId2, probe.ref, probe.ref),
        event = CreateGameEvent(gameId, playerId1, playerId2, probe.ref),
        state = setupState,
        resultMsgs = Seq(CreateGameResultMsg(gameId, playerId1, playerId2, success = true, probe.ref))
      )

      val setup1State = setupState.copy(
        data = setupState.data.updated(
          playerId1, setupState.data(playerId1).copy(ships = ships, setupShips = true)
        )
      )

      execCmd(
        cmd = SetupGameCmd(playerId1, ships, probe.ref),
        event = SetupGameEvent(gameId, playerId1, ships),
        state = setup1State,
        resultMsgs = Seq(SetupGameResultMsg(gameId, playerId1, success = true))
      )

      val setup2State = setup1State.copy(
        data = setup1State.data.updated(
          playerId2, setup1State.data(playerId2).copy(ships = ships, setupShips = true)
        )
      )
      val battleState = Battle(gameId, playerId1, setup2State.data, List())

      execCmd(
        cmd = SetupGameCmd(playerId2, ships, probe.ref),
        event = SetupGameEvent(gameId, playerId2, ships),
        state = battleState,
        resultMsgs = Seq(SetupGameResultMsg(gameId, playerId2, success = true))
      )

      val shot11State = execShot(shotCmd11, Injured, battleState)
      val shot21State = execShot(shotCmd21, Missed, shot11State)
      val shot12State = execShot(shotCmd12, Destroyed, shot21State)
      execShot(shotCmd22, Missed, shot12State)

      execCmd(
        cmd = shotCmd13,
        event = ShotEvent(shotCmd13.playerId, shotCmd13.x, shotCmd13.y),
        state = EndGame(gameId, Some(playerId1)),
        resultMsgs = Seq(ShotResultMsg(gameId, playerId1, shotCmd13.x, shotCmd13.y , Won.toString), GameResultMsg(gameId, Some(playerId1)))
      )

    }

    "ships setup timeout" in {
      val eventSourcedTestKit: EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State] =
        EventSourcedBehaviorTestKit(system, Game("entityId1", 500.millis, 5.seconds, Rules.fieldWidth, Rules.fieldHeight, shipRules))

      eventSourcedTestKit.runCommand(CreateGameCmd(gameId, playerId1, playerId2, probe.ref, probe.ref))
      probe.expectMessage(CreateGameResultMsg(gameId, playerId1, playerId2, success = true, probe.ref))
      probe.expectNoMessage()

      eventSourcedTestKit.runCommand(SetupGameCmd(playerId1, ships, probe.ref))
      probe.expectMessage(SetupGameResultMsg(gameId, playerId1, success = true))

      Thread.sleep(600)
      //Waiting for setup from the second player, a timeout occurs and Game finishing
      probe.expectMessage(GameResultMsg(gameId, None))
      probe.expectNoMessage()
    }

    "reject shot not in your turn" in {
      implicit val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, State] =
        createAndSetupGame(5.seconds)

      shotCmd(playerId1, 1, 1, Missed)
      shotCmd(playerId1, 1, 1, NotYourTurn)
      probe.expectNoMessage()
    }

    "shot timeout" in {
      implicit val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, State] =
        createAndSetupGame(500.millis)

      shotCmd(playerId1, 1, 1, Missed)

      Thread.sleep(600)

      shotCmd(playerId2, 1, 1, NotYourTurn)
      shotCmd(playerId1, 1, 1, Missed)
      probe.expectNoMessage()
    }
  }


  def execCmd(cmd: Game.Command, event: Game.Event, state: Game.State, resultMsgs: Seq[Manager.Result])
             (implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State]): Unit = {
    val cmdResult = eventSourcedTestKit.runCommand(cmd)
    cmdResult.event shouldBe event
    cmdResult.state shouldBe state

    resultMsgs.foreach(msg => probe.expectMessage(msg))
    probe.expectNoMessage()
  }

  def execShot(shotCmd: ShotCmd, shotResult: ShotResult, battle: Battle)
              (implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State]): Battle = {
    val newState = battle.copy(
      //change of mover
      moverId = if (battle.moverId == playerId1) playerId2 else playerId1,
      //add shot
      data = battle.data.updated(
        battle.moverId, battle.data(battle.moverId).copy(shots = battle.data(battle.moverId).shots :+ Shot(shotCmd.x, shotCmd.y))
      ))

    execCmd(
      cmd = shotCmd,
      event = ShotEvent(shotCmd.playerId, shotCmd.x, shotCmd.y),
      state = newState,
      resultMsgs = Seq(ShotResultMsg(gameId, shotCmd.playerId, shotCmd.x, shotCmd.y, shotResult.toString))
    )

    newState
  }

  def createAndSetupGame(moveTimeout: FiniteDuration): EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State]= {
    val eventSourcedTestKit: EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State] =
      EventSourcedBehaviorTestKit(system, Game("entityId1", 10.seconds, moveTimeout, Rules.fieldWidth, Rules.fieldHeight, shipRules))

    eventSourcedTestKit.runCommand(CreateGameCmd(gameId, playerId1, playerId2, probe.ref, probe.ref))
    probe.expectMessage(CreateGameResultMsg(gameId, playerId1, playerId2, success = true, probe.ref))
    probe.expectNoMessage()

    eventSourcedTestKit.runCommand(SetupGameCmd(playerId1, ships, probe.ref))
    probe.expectMessage(SetupGameResultMsg(gameId, playerId1, success = true))
    probe.expectNoMessage()

    eventSourcedTestKit.runCommand(SetupGameCmd(playerId2, ships, probe.ref))
    probe.expectMessage(SetupGameResultMsg(gameId, playerId2, success = true))

    eventSourcedTestKit
  }

  def shotCmd(playerId: PlayerId, x: Int, y: Int, shotResult: ShotResult)
             (implicit eventSourcedTestKit: EventSourcedBehaviorTestKit[Game.Command, Game.Event, Game.State]): Unit = {
    eventSourcedTestKit.runCommand(ShotCmd(playerId, x, y, probe.ref, probe.ref))
    probe.expectMessage(ShotResultMsg(gameId, playerId, x, y, shotResult.toString))
  }

}
