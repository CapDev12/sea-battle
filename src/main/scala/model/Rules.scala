package model

object Rules {

  val fieldWidth = 10
  val fieldHeight = 10

  case class ShipRule(decks: Byte, count: Int)
  type ShipRules = Set[ShipRule]

  val ships: ShipRules = Set(
    ShipRule(4, 1),
    ShipRule(3, 2),
    ShipRule(2, 3),
    ShipRule(1, 4)
  )
}
