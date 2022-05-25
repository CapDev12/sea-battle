package model

import model.Shots.Shot
import utils.BattleMath

object Ships {
  sealed trait Direction
  case object Horizontal extends Direction
  case object Vertical extends Direction

  case class Ship(x: Int, y: Int, dir: Boolean, decks: Byte)

  def calcShipsDecks(ships: Seq[Ship]): Set[(Byte, Int)] =
    ships
      .groupBy(_.decks)
      .map { case (decks, ships) => (decks, ships.size) }
      .toSet

  def checkShipsDecks(ships: Seq[Ship]): Boolean =
    Ships.calcShipsDecks(ships) == Rules.ships

  def checkShipBounds(ship: Ship): Boolean =
    BattleMath.cells(ship).forall { case Shot(x, y) => x >= 1 && x <= Rules.fieldWidth && y >= 1 && y <= Rules.fieldHeight }

  def checkShipsBounds(ships: Seq[Ship]): Boolean =
    ships.forall(checkShipBounds)

}
