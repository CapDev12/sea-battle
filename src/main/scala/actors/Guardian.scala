package actors

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import grpc.GrpcServer

import scala.concurrent.duration.FiniteDuration

object Guardian {

  def apply(apiEnabled: Boolean, useClustrListener: Boolean, interface: String, port: Int, askTimeout: Timeout,
            setupTimeout: FiniteDuration, moveTimeout: FiniteDuration): Behavior[NotUsed] =
    Behaviors.setup { context =>
      if (useClustrListener)
        context.spawn(ClusterListener(), "ClusterListener")

      val gameSharding = GameSharding(ClusterSharding(context.system), setupTimeout, moveTimeout)
      gameSharding.initSharding()

      if (apiEnabled) {
        val managerActor = context.spawn(Manager(gameSharding), "Manager")
        GrpcServer(interface, port, askTimeout, managerActor)(context.system)
      }

      Behaviors.same
    }

}
