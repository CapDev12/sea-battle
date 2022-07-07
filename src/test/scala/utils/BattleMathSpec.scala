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

  "randormPlaceShipsToField" must {
    "places ships according to the rules" in {
      val ships = BattleMath.randormPlaceShipsToField(Rules.fieldWidth, Rules.fieldHeight, Rules.ships)
      val result = BattleMath.checkShipsDecks(Rules.ships, ships) &&
        BattleMath.checkShipsBoundsAndBorders(Rules.fieldWidth, Rules.fieldHeight, ships)
      result shouldBe true
    }
  }
}
