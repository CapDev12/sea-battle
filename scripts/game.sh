#bin/bash

gameId=$(grpcurl -d '{"player1": {"id": "4e0453ee-c7ba-11ec-9d64-0242ac120002"}, "player2": {"id": "541042b6-c7ba-11ec-9d64-0242ac120002"}}' -plaintext \
-import-path /home/capdev/Projects/sea-battle/src/main/protobuf \
-proto battle.proto \
$host:8080 battle.BattleService/start | jq -r '.gameId')

echo "Started game $gameId"
