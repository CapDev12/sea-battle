package model

object Ships {
  sealed trait Direction
  case object Horizontal extends Direction
  case object Vertical extends Direction

  case class Ship(x: Int, y: Int, dir: Boolean, decks: Byte)

}
