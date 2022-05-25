import actors.Manager
import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import battle.BattleServiceHandler
import com.typesafe.config.ConfigFactory
import grpc.BattleServiceImpl
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object Main extends App {

 implicit val actorSystem: ActorSystem[Manager.Message] = ActorSystem(Manager(), "ActorSystem")

  val config = ConfigFactory.load()
  actorSystem.log.info(s"akka.remote.artery.canonical.port: ${config.getString("akka.remote.artery.canonical.port")}")
//  actorSystem.log.info(s"akka.cluster.seed1: ${config.getString("akka.cluster.seed1")}")
//  actorSystem.log.info(s"akka.cluster.seed2: ${config.getString("akka.cluster.seed2")}")
  actorSystem.log.info(s"akka.cluster.seed-nodes: ${config.getStringList("akka.cluster.seed-nodes")}")

  val createIfNotExists: Future[Done] = SchemaUtils.createIfNotExists()(actorSystem.classicSystem)
  val result = Await.result(createIfNotExists, 1.minutes)
  actorSystem.log.info(s"Database initialized result: $result")

  val managerActor: ActorRef[Manager.Message] = actorSystem



  Http()
    .newServerAt(interface = config.getString("grpc.interface"), port = config.getInt("grpc.port"))
    .bind(BattleServiceHandler(new BattleServiceImpl(managerActor)(actorSystem.scheduler))(actorSystem.classicSystem))

}
