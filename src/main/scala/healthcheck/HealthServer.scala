package healthcheck

import actors.Manager
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.typed.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, path}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout

import scala.concurrent.Future

class HealthServer(system: ActorSystem[_]) {

  def apply(interface: String, port: Int)(implicit system: ActorSystem[_]): Future[Http.ServerBinding] = {
    val cluster = Cluster(system)
    val healthRoutes: Route = Directives.concat(
      path("healthcheck") {
        Directives.get {
          complete(StatusCodes.OK)
          //      if (loaded.get) complete(StatusCodes.OK)
          //      else complete(StatusCodes.ServiceUnavailable)
        }
      },
        path("readiness") {
          Directives.get {
            complete(StatusCodes.OK)

            //      if (loaded.get) complete(StatusCodes.OK)
            //      else complete(StatusCodes.ServiceUnavailable)
          }
        }
    )
    Http()
      .newServerAt(interface, port)
      .bind(healthRoutes)
  }

}
