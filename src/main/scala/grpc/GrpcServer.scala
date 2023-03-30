package grpc

import actors.Manager
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.util.Timeout
import battle.BattleServiceHandler

import scala.concurrent.Future

object GrpcServer {

  def apply(interface: String, port: Int, timeout: Timeout, managerActor: ActorRef[Manager.Message])(implicit system: ActorSystem[_]): Future[Http.ServerBinding] = {
    val battleServiceHandler = BattleServiceHandler(new BattleServiceImpl(managerActor, timeout)(system.scheduler))(system.classicSystem)

    Http()
      .newServerAt(interface, port)
      .bind(battleServiceHandler)
  }

}
