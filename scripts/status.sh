grpcurl -d '{"gameId": "a71c6884-98e1-438d-8bd4-43d2f11be681", "playerId": "4e0453ee-c7ba-11ec-9d64-0242ac120002"}' -plaintext \
-import-path /home/capdev/Projects/sea-battle/src/main/protobuf \
-proto battle.proto \
$host:8080 battle.BattleService/status