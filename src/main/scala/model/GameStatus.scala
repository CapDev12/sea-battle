package model

object GameStatus {
  type GameStatus = Int
  val NotFound = 1
  val Wait = 2
  val Turn = 3
  val Won = 4
  val Lose = 5

  def gameStatusToStr(gameStatus: GameStatus): String = gameStatus match {
    case NotFound => "NotFound"
    case Wait => "Wait"
    case Turn => "Turn"
    case Won => "Won"
    case Lose => "Lose"
  }

}
