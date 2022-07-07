package utils

import model.Ships.Ship
import model.Shots._

import java.util.Random
import scala.annotation.tailrec

object BattleMath {

  def cells(ship: Ship, withBorder: Boolean = false, fieldWidth: Int = 10, fieldHeight: Int = 10): Set[Shot] = {
    def cellBorder(x: Int, y: Int): Seq[Shot] =
      Seq(
        Shot(x - 1, y - 1),
        Shot(x, y - 1),
        Shot(x + 1, y - 1),
        Shot(x + 1, y),
        Shot(x + 1, y + 1),
        Shot(x, y + 1),
        Shot(x - 1, y + 1),
        Shot(x - 1, y)
      )

    (0 until ship.decks)
      .flatMap { deck =>
        val (x, y) = if (ship.dir)
          (ship.x, ship.y + deck)
        else
          (ship.x + deck, ship.y)

        val border = if (withBorder) cellBorder(x, y) else Seq.empty[Shot]
        Shot(x, y) +: border
      }
      .toSet
      .filter { case Shot(x, y) => x >=1 && x <= fieldWidth && y >=1 && y <= fieldHeight}
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
      hitedShip
        .map(ship => hitShip(ship, shots, shot))
        .getOrElse(Missed)
    }
  }

  private def calculateShipsDecks(ships: Seq[Ship]): Set[(Byte, Int)] =
    ships
      .groupBy(_.decks)
      .map { case (decks, ships) => (decks, ships.size) }
      .toSet

  def checkShipsDecks(rulesShips: Set[(Byte, Int)], ships: Seq[Ship]): Boolean =
    calculateShipsDecks(ships) == rulesShips

  private def checkShipBound(fieldWidth: Int, fieldHeight: Int, ship: Ship): Boolean =
    BattleMath.cells(ship).forall { case Shot(x, y) => x >= 1 && x <= fieldWidth && y >= 1 && y <= fieldHeight }

  private def findIntersectShipsBorders(fieldWidth: Int, fieldHeight: Int, ships: Seq[Ship]): Set[(Ship, Ship)] = {
    def isIntersect(fieldWidth: Int, fieldHeight: Int, ship1: Ship, ship2: Ship): Boolean =
      cells(ship1, withBorder = true, fieldWidth, fieldHeight).intersect(cells(ship2)) != Set.empty

    (for {
      ship1 <- ships
      ship2 <- ships if ship1 != ship2
    } yield (ship1, ship2, isIntersect(fieldWidth, fieldHeight, ship1, ship2)))
      .filter { case (_, _, itersect) => itersect }
      .map { case (ship1, ship2, _) => (ship1, ship2) }
      .toSet
  }

  def checkShipsBoundsAndBorders(fieldWidth: Int, fieldHeight: Int, ships: Seq[Ship]): Boolean = {
    val intersectShipsBorders = findIntersectShipsBorders(fieldWidth, fieldHeight, ships)
    if(intersectShipsBorders.nonEmpty) println(s"diff: $intersectShipsBorders")

    ships.forall(checkShipBound(fieldWidth, fieldHeight, _)) && intersectShipsBorders.isEmpty
  }

  lazy val random = new Random()

  def randormPlaceShipsToField(fieldWidth: Int, fieldHeight: Int, ships: Set[(Byte, Int)]): Seq[Ship] = {

    def isShipIntersect(ships: Seq[Ship], ship: Ship): Boolean =
      ships.exists(BattleMath.cells(_, withBorder = true, fieldWidth, fieldHeight).intersect(BattleMath.cells(ship)) != Set.empty)

    @tailrec
    def place(fieldWidth: Int, fieldHeight: Int, placeShips: Seq[Byte], ships: Seq[Ship], rndInt: Int => Int): Seq[Ship] = placeShips match {
      case head :: tail =>
        val x = rndInt(fieldWidth) + 1
        val y = rndInt(fieldHeight) + 1
        val dir = if(rndInt(2) == 0) false else true
        val decks = head
        val ship = Ship(x, y, dir, decks)

        if(checkShipBound(fieldWidth, fieldHeight, ship) && !isShipIntersect(ships, ship))
          place(fieldWidth, fieldHeight, tail, ships :+ ship, rndInt)
        else
          place(fieldWidth, fieldHeight, placeShips, ships, rndInt)

      case Nil =>
        ships
    }

    val placeShips = ships
      .toSeq
      .flatMap { case (decks, count) => Seq.fill(count)(decks) }
      .sorted(Ordering[Byte].reverse)

    place(fieldWidth, fieldHeight, placeShips, Seq.empty, random.nextInt)
  }

}
