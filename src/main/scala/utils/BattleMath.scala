package utils

import model.Ships._
import model.Shots._

object BattleMath {

  def cells(ship: Ship): Set[Shot] = {
    if (ship.dir) {
      (0 until ship.decks).map(deck => Shot(ship.x, ship.y + deck)).toSet
    } else {
      (0 until ship.decks).map(deck => Shot(ship.x + deck, ship.y)).toSet
    }
  }

  def isDestroy(ship: Ship, shots: Set[Shot]): Boolean =
    cells(ship).intersect(shots).size == ship.decks

  def hitShip(ship: Ship, shots: Set[Shot], shot: Shot): ShotResult = {
    if(!cells(ship).contains(shot))
      Missed
    else if (isDestroy(ship, shots + shot))
      Destroyed
    else
      Injured
  }

  def hit(ships: Seq[Ship], shots: Set[Shot], shot: Shot): ShotResult = {
    val won = ships.forall(isDestroy(_, shots + shot))

    if (won)
      Won
    else {
      val hitedShip = ships.find(ship => hitShip(ship, shots, shot) != Missed)
      hitedShip.map(ship => hitShip(ship, shots, shot)).getOrElse(Missed)
    }
  }

}
