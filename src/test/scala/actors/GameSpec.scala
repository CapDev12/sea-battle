package actors

import actors.Game.{Battle, CreateGameCmd, CreateGameEvent, EndGame, Init, Setup, SetupGameCmd, SetupGameEvent, ShotCmd, ShotEvent}
import actors.Manager.{CreateGameResultMsg, GameResultMsg, Message, SetupGameResultMsg, ShotResultMsg}
import akka.Done
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing, ScalaTestWithActorTestKit, TestProbe}
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import model.Games.GameId
import model.Players.PlayerData
import model.Rules
import model.Rules.{ShipRule, ShipRules}
import model.Ships.Ship
import model.Shots.{Destroyed, Injured, Missed, Shot, ShotResult, Won}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import utils.BattleMath
import utils.Utils.uuid

import scala.concurrent.duration.DurationInt

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
        EventSourcedBehaviorTestKit(system, Game("entityId1", 100.millis, 5.seconds, Rules.fieldWidth, Rules.fieldHeight, shipRules))

      eventSourcedTestKit.runCommand(CreateGameCmd(gameId, playerId1, playerId2, probe.ref, probe.ref))
      probe.expectMessage(CreateGameResultMsg(gameId, playerId1, playerId2, success = true, probe.ref))
      probe.expectNoMessage()

      eventSourcedTestKit.runCommand(SetupGameCmd(playerId1, ships, probe.ref))
      probe.expectMessage(SetupGameResultMsg(gameId, playerId1, success = true))

      Thread.sleep(100)
      //Waiting for setup from the second player, a timeout occurs and Game finishing
      probe.expectMessage(GameResultMsg(gameId, None))
      probe.expectNoMessage()
    }

//    "skip shot not in your turn" in {
//      val game = testKit.spawn(Game(data, probe.ref))
//
//      val ShotCmd11 = ShotCmd(playerId1, 1, 1)
//      val ShotCmd21 = ShotCmd(playerId2, 3, 3)
//      val ShotCmd12 = ShotCmd(playerId1, 1, 2)
//      val ShotCmd22 = ShotCmd(playerId2, 2, 1)
//
//      game ! ShotCmd11
//      probe.expectMessage(ShotResultEvent(ShotCmd11, Injured))
//      game ! ShotCmd21
//      game ! ShotCmd22
//      probe.expectMessage(ShotResultEvent(ShotCmd21, Missed))
//      probe.expectNoMessage()
//      game ! ShotCmd12
//      probe.expectMessage(ShotResultEvent(ShotCmd12, Destroyed))
//    }

  }

  def shotToResult(gameId: GameId, shot: ShotCmd, result: String): ShotResultMsg =
    ShotResultMsg(gameId, shot.playerId, shot.x, shot.y, result)

  def shotCmdToShot(shotCmd: ShotCmd): Shot =
    Shot(shotCmd.x, shotCmd.y)

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
        battle.moverId, battle.data(battle.moverId).copy(shots = battle.data(battle.moverId).shots :+ shotCmdToShot(shotCmd))
      ))

    execCmd(
      cmd = shotCmd,
      event = ShotEvent(shotCmd.playerId, shotCmd.x, shotCmd.y),
      state = newState,
      resultMsgs = Seq(shotToResult(gameId, shotCmd, shotResult.toString))
    )

    newState
  }

}
