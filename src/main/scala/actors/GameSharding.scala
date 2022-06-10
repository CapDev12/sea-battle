package actors

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}

case class GameSharding(sharding: ClusterSharding) {
  val TypeKey: EntityTypeKey[Game.Command] = EntityTypeKey[Game.Command]("Game")

  def initSharding(): Unit =
    sharding.init(Entity(TypeKey)(createBehavior = entityContext => Game(entityContext.entityId)))

  def entityRefFor(entityId: String): EntityRef[Game.Command] =
    sharding.entityRefFor(TypeKey, entityId)

}
