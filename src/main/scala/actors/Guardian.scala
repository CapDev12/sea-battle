package actors

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout
import grpc.GrpcServer
import model.Rules.ShipRules

import scala.concurrent.duration.FiniteDuration

object Guardian {

  def apply(apiEnabled: Boolean, useClustrListener: Boolean, interface: String, port: Int, askTimeout: Timeout,
            setupTimeout: FiniteDuration, moveTimeout: FiniteDuration, fieldWidth: Int, fieldHeight: Int,
            shipRules: ShipRules): Behavior[NotUsed] =
    Behaviors.setup { context =>
      implicit val system: ActorSystem[_] = context.system
      if (useClustrListener)
        context.spawn(ClusterListener(), "ClusterListener")

      val gameSharding: GameSharding =
        GameShardingImpl(ClusterSharding(context.system), setupTimeout, moveTimeout, fieldWidth, fieldHeight, shipRules)

      if (apiEnabled) {
        val managerActor = context.spawn(Manager(gameSharding), "Manager")
        GrpcServer(interface, port, askTimeout, managerActor)
      }

      AkkaManagement(system).start()

      Behaviors.same
    }

}
