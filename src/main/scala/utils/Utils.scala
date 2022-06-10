package utils

import akka.util.Timeout

import java.util.UUID
import java.time.Duration
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Utils {

  def uuid: UUID = UUID.randomUUID()

  implicit def durationToTimeout(duration: Duration): Timeout =
    Timeout(duration.toMillis, MILLISECONDS)

  implicit def durationToFiniteDuration(duration: Duration): FiniteDuration =
    FiniteDuration(duration.toMillis, MILLISECONDS)

}
