package grpc

import actors.Manager
import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.util.Timeout
import battle.BattleServiceHandler

object GrpcServer {

  def apply(interface: String, port: Int, timeout: Timeout, managerActor: ActorRef[Manager.Message])(implicit system: ActorSystem[_]): Unit = {
    val battleHandler = BattleServiceHandler(new BattleServiceImpl(managerActor, timeout)(system.scheduler))(system.classicSystem)
    Http()
      .newServerAt(interface, port)
      .bind(battleHandler)
  }

}
