package model

object Shots {

  sealed trait ShotResult

  case object GameNotFound extends ShotResult
  case object Missed extends ShotResult
  case object Injured  extends ShotResult
  case object Destroyed extends ShotResult
  case object Won extends ShotResult
  case object Lose extends ShotResult
  case object NotYourTurn extends ShotResult
  case object Timeout extends ShotResult

  case class Shot(x: Int, y: Int)

}
