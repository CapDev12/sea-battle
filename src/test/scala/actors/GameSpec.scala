package actors

import actors.Game.ShotCmd
import actors.Manager.{GameClose, ShotResultEvent}
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import model.Players.{PlayerData, PlayerId}
import model.Ships.{Horizontal, Ship, Vertical}
import model.Shots.{Destroyed, Injured, Missed, Shot}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utils.Utils.uuid

import scala.concurrent.duration.DurationInt

class GameSpec
  extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  val playerId1 = uuid
  val playerId2 = uuid

  val probe = testKit.createTestProbe[Manager.ManagerEvent]("manager")

//  val data = Map[PlayerId, PlayerData](
//    (playerId1, PlayerData(Seq(Ship(1, 1, Horizontal, 2)), Set())),
//    (playerId2, PlayerData(Seq(Ship(1, 1, Vertical, 2)), Set()))
//  )

  "Game" must {

//    "follow the life cycle" in {
//      val game = testKit.spawn(Game(data, probe.ref))
//
//      val shotEvent11 = ShotEvent(playerId1, 1, 1)
//      val shotEvent21 = ShotEvent(playerId2, 3, 3)
//      val shotEvent12 = ShotEvent(playerId1, 1, 2)
//      val shotEvent22 = ShotEvent(playerId2, 2, 1)
//
//      game ! shotEvent11
//      probe.expectMessage(ShotResultEvent(shotEvent11, Injured))
//      game ! shotEvent21
//      probe.expectMessage(ShotResultEvent(shotEvent21, Missed))
//      game ! shotEvent12
//      probe.expectMessage(ShotResultEvent(shotEvent12, Destroyed))
//      game ! shotEvent22
//      probe.expectMessage(GameEnd(playerId1))
//      probe.expectNoMessage()
//      probe.expectTerminated(game)
//    }
//
//    "return shot timeout" in {
//      val game = testKit.spawn(Game(data, probe.ref))
//
//      val shotEvent11 = ShotEvent(playerId1, 1, 1)
//      val shotEvent12 = ShotEvent(playerId1, 2, 1)
//
//      game ! shotEvent11
//      probe.expectMessage(ShotResultEvent(shotEvent11, Injured))
//      probe.expectMessage(6.second, ShotTimeoutEvent(playerId2))
//      game ! shotEvent12
//      probe.expectMessage(ShotResultEvent(shotEvent12, Missed))
//    }
//
//    "skip shot not in your turn" in {
//      val game = testKit.spawn(Game(data, probe.ref))
//
//      val shotEvent11 = ShotEvent(playerId1, 1, 1)
//      val shotEvent21 = ShotEvent(playerId2, 3, 3)
//      val shotEvent12 = ShotEvent(playerId1, 1, 2)
//      val shotEvent22 = ShotEvent(playerId2, 2, 1)
//
//      game ! shotEvent11
//      probe.expectMessage(ShotResultEvent(shotEvent11, Injured))
//      game ! shotEvent21
//      game ! shotEvent22
//      probe.expectMessage(ShotResultEvent(shotEvent21, Missed))
//      probe.expectNoMessage()
//      game ! shotEvent12
//      probe.expectMessage(ShotResultEvent(shotEvent12, Destroyed))
//    }

  }

}
