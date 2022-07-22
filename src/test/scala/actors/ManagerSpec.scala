package actors

import actors.Game.{CreateGameCmd, SetupGameCmd, ShotCmd, WatchCmd}
import actors.Manager._
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import model.Rules
import model.Ships.Ship
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utils.Utils.uuid
import utils.{BattleMath, GameShardingStubImpl}

class ManagerSpec extends AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  private val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  private val playerId1 = uuid
  private val playerId2 = uuid

  private val ships: Seq[Ship] = BattleMath.randormPlaceShipsToField(Rules.fieldWidth, Rules.fieldHeight, Rules.ships)

  private val gameProbe = testKit.createTestProbe[Game.Command]()
  private val replyToProbe = testKit.createTestProbe[Manager.Result]()
  private val watchProbe = akka.testkit.TestProbe()(testKit.system.classicSystem)

  private val gameShardingStub = GameShardingStubImpl(gameProbe.ref)
  private val manager = testKit.spawn(Manager(gameShardingStub), "manager")

  "Manager" must {

    "handle life cycle" in {

      //Create Game
      manager ! CreateGameMsg(playerId1, playerId2, replyToProbe.ref)

      val createGameCmd = gameProbe.expectMessageType[CreateGameCmd]
      createGameCmd.playerId1 shouldBe playerId1
      createGameCmd.playerId2 shouldBe playerId2
      createGameCmd.managerRef shouldBe manager.ref
      createGameCmd.replyTo shouldBe replyToProbe.ref

      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()

      val gameId = createGameCmd.gameId

      manager ! CreateGameResultMsg(gameId, playerId1, playerId2, success = true, replyToProbe.ref)

      replyToProbe.expectMessage(
        CreateGameResultMsg(gameId, playerId1, playerId2, success = true, replyToProbe.ref)
      )
      replyToProbe.expectNoMessage()

      //Setup Ships player1
      manager ! SetupGameMsg(gameId, playerId1, ships, replyToProbe.ref)

      gameProbe.expectMessage(
        SetupGameCmd(playerId1, ships, replyToProbe.ref)
      )
      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()

      //Setup Ships player2
      manager ! SetupGameMsg(gameId, playerId2, ships, replyToProbe.ref)

      gameProbe.expectMessage(
        SetupGameCmd(playerId2, ships, replyToProbe.ref)
      )

      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()

      //Shot
      manager ! ShotMsg(gameId, playerId1, 1, 2, replyToProbe.ref)

      gameProbe.expectMessage(
        ShotCmd(playerId1, 1, 2, replyToProbe.ref, manager.unsafeUpcast)
      )
      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()

      //Watch by events
      manager ! WatchMsg(gameId, playerId1, watchProbe.ref)

      gameProbe.expectMessage(WatchCmd(watchProbe.ref))
      gameProbe.expectNoMessage()
      watchProbe.expectNoMessage()

      //Finish Game, won player1
      manager ! GameResultMsg(gameId, Some(playerId1))
      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()

      //Setup ships in a non-existent game
      manager ! SetupGameMsg(gameId, playerId1, ships, replyToProbe.ref)

      replyToProbe.expectMessage(
        SetupGameResultMsg(gameId, playerId1, success = false)
      )
      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()

      //Shot in a game that doesn't exist
      manager ! ShotMsg(gameId, playerId1, 1, 2, replyToProbe.ref)

      replyToProbe.expectMessage(
        ShotResultMsg(gameId, playerId1, 1, 2, "GameNotFound")
      )
      gameProbe.expectNoMessage()
      replyToProbe.expectNoMessage()
    }
  }
}