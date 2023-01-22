import actors.Guardian
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import com.typesafe.config.ConfigFactory
import model.Rules

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load()
  
  import utils.Utils.{durationToFiniteDuration, durationToTimeout}
  implicit val system: ActorSystem[NotUsed] = ActorSystem(
    Guardian(
      apiEnabled = config.getBoolean("api.enabled"),
      useClustrListener = true,
      interface = config.getString("grpc.interface"),
      port = config.getInt("grpc.port"),
      askTimeout = config.getDuration("game.ask-timeout"),
      setupTimeout = config.getDuration("game.setup-timeout"),
      moveTimeout = config.getDuration("game.move-timeout"),
      fieldWidth = Rules.fieldWidth,
      fieldHeight = Rules.fieldHeight: Int,
      shipRules = Rules.ships
    ), "ActorSystem")

  SchemaUtils
    .createIfNotExists()
    .onComplete {
      case Success(_) =>
        system.log.info("Database initialized successfully")
      case Failure(ex) =>
        system.log.info("An error occurred while initializing the database: " + ex.getMessage)
    }
}
