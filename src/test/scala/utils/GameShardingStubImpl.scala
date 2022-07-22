package utils

import actors.{Game, GameSharding}
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.{EntityRef, EntityTypeKey}
import akka.cluster.sharding.typed.testkit.scaladsl.TestEntityRef

case class GameShardingStubImpl(probe: ActorRef[Game.Command]) extends GameSharding {

  val TypeKey: EntityTypeKey[Game.Command] = EntityTypeKey[Game.Command]("Game")

  override def entityRefFor(entityId: String): EntityRef[Game.Command] =
    TestEntityRef(TypeKey, entityId, probe)

}
