#!/bin/bash
#set -x

host=localhost
port=8080
d=0
d1=0

function grpc {
  grpcurl -d "$1" -plaintext \
  -import-path /home/capdev/Projects/sea-battle/src/main/protobuf \
  -proto battle.proto \
  $host:$port battle.BattleService/$2
}

gameId=eab48de0-80a0-45a1-af1f-e4b663949996

grpc '{"gameId": "'$gameId'", "playerId": "541042b6-c7ba-11ec-9d64-0242ac120002", "ships": [{"x": 1, "y": 1, "dir": "Horisontal", "decks": 4},{"x": 1, "y": 3, "dir": "Horisontal", "decks": 3},{"x": 4, "y": 3, "dir": "Horisontal", "decks": 3},{"x": 1, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 4, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 7, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 1, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 3, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 5, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 7, "y": 7, "dir": "Horisontal", "decks": 1}]}' setup
