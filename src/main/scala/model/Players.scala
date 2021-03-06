package model

import model.Ships.Ship
import model.Shots.Shot

import java.util.UUID

object Players {
  type PlayerId = UUID

  case class PlayerData(ships: Seq[Ship], shots: Seq[Shot], setupShips: Boolean = false)

  type PlayersData = Map[PlayerId, PlayerData]

  def initData(playerId1: PlayerId, playerId2: PlayerId): PlayersData =
    Map(
      playerId1 -> PlayerData(Seq(), Seq()),
      playerId2 -> PlayerData(Seq(), Seq())
    )

}
