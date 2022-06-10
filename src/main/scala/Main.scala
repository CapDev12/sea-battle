import actors.Guardian
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load()

  import utils.Utils.{durationToTimeout, durationToFiniteDuration}
  implicit val system: ActorSystem[NotUsed] = ActorSystem(
    Guardian(
      apiEnabled = config.getBoolean("api.enabled"),
      useClustrListener = true,
      interface = config.getString("grpc.interface"),
      port = config.getInt("grpc.port"),
      askTimeout = config.getDuration("game.ask-timeout"),
      setupTimeout = config.getDuration("game.setup-timeout"),
      moveTimeout = config.getDuration("game.move-timeout")
    ), "ActorSystem")

  SchemaUtils
    .createIfNotExists()
    .onComplete {
      case Success(_) =>
        system.log.info("createIfNotExists: Database initialized")
      case Failure(ex) =>
        system.log.info("An error has occurred: " + ex.getMessage)
    }
}
