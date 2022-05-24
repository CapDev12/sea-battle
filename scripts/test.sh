#!/bin/bash
#set -x

host=msi
port=8080
d=0
d1=0

function grpc {
  grpcurl -d "$1" -plaintext \
  -import-path /home/capdev/Projects/sea-battle/src/main/protobuf \
  -proto battle.proto \
  $host:$port battle.BattleService/$2
}

gameId=$(grpc '{"player1": {"id": "4e0453ee-c7ba-11ec-9d64-0242ac120002"}, "player2": {"id": "541042b6-c7ba-11ec-9d64-0242ac120002"}}' start | jq -r '.gameId')
echo "Started game $gameId"

#sleep $d
#gnome-terminal -- sh -c "grpcurl -d '{\"gameId\": \"$gameId\", \"playerId\": \"4e0453ee-c7ba-11ec-9d64-0242ac120002\"}' -plaintext -import-path /home/capdev/Projects/sea-battle/src/main/protobuf -proto battle.proto $host:8080 battle.BattleService/moves"
#gnome-terminal -- sh -c "grpcurl -d '{\"gameId\": \"$gameId\", \"playerId\": \"541042b6-c7ba-11ec-9d64-0242ac120002\"}' -plaintext -import-path /home/capdev/Projects/sea-battle/src/main/protobuf -proto battle.proto $host:8080 battle.BattleService/moves"

sleep $d
grpc '{"gameId": "'$gameId'", "playerId": "4e0453ee-c7ba-11ec-9d64-0242ac120002", "ships": [{"x": 1, "y": 1, "dir": "Horisontal", "decks": 4},{"x": 1, "y": 3, "dir": "Horisontal", "decks": 3},{"x": 4, "y": 3, "dir": "Horisontal", "decks": 3},{"x": 1, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 4, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 7, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 1, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 3, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 5, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 7, "y": 7, "dir": "Horisontal", "decks": 1}]}' setup
sleep $d
grpc '{"gameId": "'$gameId'", "playerId": "541042b6-c7ba-11ec-9d64-0242ac120002", "ships": [{"x": 1, "y": 1, "dir": "Horisontal", "decks": 4},{"x": 1, "y": 3, "dir": "Horisontal", "decks": 3},{"x": 4, "y": 3, "dir": "Horisontal", "decks": 3},{"x": 1, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 4, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 7, "y": 5, "dir": "Horisontal", "decks": 2},{"x": 1, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 3, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 5, "y": 7, "dir": "Horisontal", "decks": 1},{"x": 7, "y": 7, "dir": "Horisontal", "decks": 1}]}' setup

sleep $d

for y in {1..10}
do
  for x in {1..10}
  do
    sleep $d1
    grpc '{"gameId": "'$gameId'", "playerId": "4e0453ee-c7ba-11ec-9d64-0242ac120002", "x": '$x', "y": '$y'}' shot
    sleep $d1
    grpc '{"gameId": "'$gameId'", "playerId": "541042b6-c7ba-11ec-9d64-0242ac120002", "x": 1, "y": 1}' shot
  done
done
