package actors

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import model.Rules.ShipRules

import scala.concurrent.duration.FiniteDuration

case class GameSharding(sharding: ClusterSharding, setupTimeout: FiniteDuration, moveTimeout: FiniteDuration,
                        fieldWidth: Int, fieldHeight: Int, shipRules: ShipRules) {
  val TypeKey: EntityTypeKey[Game.Command] = EntityTypeKey[Game.Command]("Game")

  def initSharding(): Unit =
    sharding.init(Entity(TypeKey)(createBehavior = entityContext => Game(entityContext.entityId, setupTimeout, moveTimeout,
      fieldWidth, fieldHeight, shipRules)))

  def entityRefFor(entityId: String): EntityRef[Game.Command] =
    sharding.entityRefFor(TypeKey, entityId)

}
