package model

object Shots {
  type ShotResult = Int

  val GameNotFound = 0
  val Missed = 1
  val Injured = 2
  val Destroyed = 3
  val Won = 4
  val Lose = 5
  val NotYourTurn = 6
  val Timeout = 7

  def shotResultToStr(shotResult: ShotResult): String = shotResult match {
    case GameNotFound => "GameNotFound"
    case Missed => "Missed"
    case Injured => "Injured"
    case Destroyed => "Destroyed"
    case Won => "Won"
    case Lose => "Lose"
    case NotYourTurn => "NotYourTurn"
    case Timeout => "Timeout"
  }

  case class Shot(x: Int, y: Int)

  def checkShotBound(x: Int, y: Int): Boolean =
    x >= 1 && x <= Rules.width && y >= 1 && y <= Rules.height
}
