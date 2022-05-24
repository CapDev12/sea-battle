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

#gameId=203b1eb7-c967-40b8-b2a3-be36d1b3d231
#gameId=95b34d0e-bebd-4fec-98fe-c74f43211047
gameId=eab48de0-80a0-45a1-af1f-e4b663949996

grpc '{"gameId": "'$gameId'", "playerId": "541042b6-c7ba-11ec-9d64-0242ac120002", "x": 1, "y": 1}' shot
