package utils

import model.Rules
import model.Ships.Ship
import model.Shots.Shot
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class BattleMathSpec extends AnyWordSpec with Matchers {

  "cell" must {
    "return correct cells occupied by a ship" in {
      BattleMath.cells(Ship(x = 2, y = 3, dir = false, decks = 4)) shouldBe
        Set(Shot(2, 3), Shot(3, 3), Shot(4, 3), Shot(5, 3))
    }
  }

  "checkShipsDecks" must {
    "checks for missing ships" in {
      BattleMath.checkShipsDecks(Rules.ships, Seq.empty) shouldBe
        Some("Missing 1 4-deck ships. Missing 2 3-deck ships. Missing 3 2-deck ships. Missing 4 1-deck ships")
    }
    "checks for extra ships" in {
      val ships = BattleMath.randormPlaceShipsToField(Rules.fieldWidth, Rules.fieldHeight, Rules.ships)
      val extraShip = Seq(Ship(1, 1, dir = false, 4), Ship(1, 1, dir = false, 10), Ship(1, 1, dir = false, 10))
      BattleMath.checkShipsDecks(Rules.ships, ships ++ extraShip) shouldBe
        Some("Extra 1 4-deck ships. Extra 2 10-deck ships")
    }
  }

  "randormPlaceShipsToField" must {
    "places ships according to the rules" in {
      val ships = BattleMath.randormPlaceShipsToField(Rules.fieldWidth, Rules.fieldHeight, Rules.ships)
      BattleMath.checkShips(Rules.fieldWidth, Rules.fieldHeight, Rules.ships, ships) shouldBe None
    }
  }

}
