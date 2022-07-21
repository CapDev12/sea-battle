package actors

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import model.Rules.ShipRules

import scala.concurrent.duration.FiniteDuration

trait GameSharding {
  def entityRefFor(entityId: String): EntityRef[Game.Command]
}

case class GameShardingImpl(sharding: ClusterSharding, setupTimeout: FiniteDuration, moveTimeout: FiniteDuration,
                            fieldWidth: Int, fieldHeight: Int, shipRules: ShipRules) extends GameSharding {

  val TypeKey: EntityTypeKey[Game.Command] = EntityTypeKey[Game.Command]("Game")

  sharding.init(
    Entity(TypeKey)(
      createBehavior =
        entityContext => Game(entityContext.entityId, setupTimeout, moveTimeout, fieldWidth, fieldHeight, shipRules)
    )
  )

  def entityRefFor(entityId: String): EntityRef[Game.Command] =
    sharding.entityRefFor(TypeKey, entityId)

}
