package actors

import actors.Game.{CreateGameCmd, SetupGameCmd, ShotCmd}
import actors.Manager.{CreateGameResultMsg, GameResultMsg, SetupGameResultMsg, ShotResultMsg}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import model.Games.GameId
import model.Rules
import model.Ships.Ship
import model.Shots.{Destroyed, Injured, Missed}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utils.BattleMath
import utils.Utils.uuid

import scala.concurrent.duration.DurationInt

class GameSpec
  extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  val gameId = uuid
  val playerId1 = uuid
  val playerId2 = uuid

  val probe = testKit.createTestProbe[Manager.Result]("manager")

  val ships1 = Seq(Ship(1, 1, true, 2))
  val ships2 = Seq(Ship(1, 1, false, 2))

  "Game" must {

    "follow the life cycle" in {
      val game = testKit.spawn(Game("entryId1", 10.seconds, 5.seconds))

      val ShotCmd11 = ShotCmd(playerId1, 1, 1, probe.ref, probe.ref)
      val ShotCmd21 = ShotCmd(playerId2, 3, 3, probe.ref, probe.ref)
      val ShotCmd12 = ShotCmd(playerId1, 1, 2, probe.ref, probe.ref)
      val ShotCmd22 = ShotCmd(playerId2, 2, 1, probe.ref, probe.ref)

      val ships = BattleMath.randormPlaceShipsToField(Rules.fieldWidth, Rules.fieldHeight, Rules.ships)

      game ! CreateGameCmd(gameId, playerId1, playerId2, probe.ref, probe.ref)
      probe.expectMessage(CreateGameResultMsg(gameId, playerId1, playerId2, success = true, probe.ref))

      game ! SetupGameCmd(playerId1, ships, probe.ref)
      probe.expectMessage(SetupGameResultMsg(gameId, playerId1, success = true))

      game ! ShotCmd11
      probe.expectMessage(shotToResult(gameId, ShotCmd11, Injured.toString))
      game ! ShotCmd21
      probe.expectMessage(shotToResult(gameId, ShotCmd11, Missed.toString))
      game ! ShotCmd12
      probe.expectMessage(shotToResult(gameId, ShotCmd11, Destroyed.toString))
      game ! ShotCmd22
      probe.expectMessage(GameResultMsg(gameId, Some(playerId1)))
      probe.expectNoMessage()
      probe.expectTerminated(game)
    }

//    "return shot timeout" in {
//      val game = testKit.spawn(Game(data, probe.ref))
//
//      val ShotCmd11 = ShotCmd(playerId1, 1, 1)
//      val ShotCmd12 = ShotCmd(playerId1, 2, 1)
//
//      game ! ShotCmd11
//      probe.expectMessage(ShotResultEvent(ShotCmd11, Injured))
//      probe.expectMessage(6.second, ShotTimeoutEvent(playerId2))
//      game ! ShotCmd12
//      probe.expectMessage(ShotResultEvent(ShotCmd12, Missed))
//    }
//
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


}
