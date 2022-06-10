package utils

import akka.util.Timeout

import java.util.UUID
import java.time.Duration
import scala.concurrent.duration.MILLISECONDS

object Utils {

  def uuid: UUID = UUID.randomUUID()

  implicit def durationToTimeout(duration: Duration): Timeout =
    Timeout(duration.toMillis, MILLISECONDS)

}
