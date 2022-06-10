import actors.Guardian
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load()

  import utils.Utils.durationToTimeout
  implicit val system: ActorSystem[NotUsed] = ActorSystem(
    Guardian(
      apiEnabled = config.getBoolean("api.enabled"),
      useClustrListener = true,
      interface = config.getString("grpc.interface"),
      port = config.getInt("grpc.port"),
      timeout = config.getDuration("grpc.timeout"),
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
