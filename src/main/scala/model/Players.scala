package model

import model.Ships.Ship
import model.Shots.Shot

import java.util.UUID

object Players {
  type PlayerId = UUID

  case class Player(id: PlayerId, name: String)
  case class PlayerData(ships: Seq[Ship], shots: Seq[Shot], setupShips: Boolean)

  type PlayersData = Map[PlayerId, PlayerData]

  def initData(playerId1: PlayerId, playerId2: PlayerId): PlayersData =
    Map(
      playerId1 -> PlayerData(Seq(), Seq(), setupShips = false),
      playerId2 -> PlayerData(Seq(), Seq(), setupShips = false)
    )

}
