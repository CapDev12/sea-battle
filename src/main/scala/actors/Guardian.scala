package actors

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import grpc.GrpcServer

object Guardian {

  def apply(useManager: Boolean, useClustrListener: Boolean, interface: String, port: Int, timeout: Timeout): Behavior[NotUsed] =
    Behaviors.setup { context =>
      if (useClustrListener)
        context.spawn(ClusterListener(), "ClusterListener")

      val gameSharding = GameSharding(ClusterSharding(context.system))
      gameSharding.initSharding()

      if (useManager) {
        val managerActor = context.spawn(Manager(gameSharding), "Manager")
        GrpcServer(interface, port, timeout, managerActor)(context.system)
      }

      Behaviors.same
    }

}
