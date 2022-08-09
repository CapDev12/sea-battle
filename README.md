## Akka Cluster Sharding example 

### Introduction

This is a Scala Sbt [Akka Cluster](https://doc.akka.io/docs/akka/current/index-cluster.html) project that demonstrates 
how to setup [Akka Cluster Sharding](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html) on the example of the sea battle game server.

This project builds up to examples of<br/> 
* [Event Sourcing](https://doc.akka.io/docs/akka/current/typed/persistence.html)<br/>
* [Command query responsibility segregation (CQRS)](https://doc.akka.io/docs/akka/2.6.19/typed/cqrs.html)<br/>
* [Akka Persistence](https://doc.akka.io/docs/akka/current/typed/index-persistence.html).

The project has the load test in the following GitHub repo [sea-battle-load-test](https://github.com/CapDev12/sea-battle-load-test)

### Compile
```bash
$ sbt compile
```

### Test
```bash
$ sbt test
```

### Deploy
This script runs MySQL database server and apps in containers :
* node1 with API route and shards
* node2 with shards
```bash
$ ./build_deploy_local.sh
```

Applications will start and see each other the cluster will start successfully
```
- Cluster Node [akka://ActorSystem@node2:2552] - Welcome from [akka://ActorSystem@node1:2551]
- Member is Up: akka://ActorSystem@node1:2551
- Member is Joined: akka://ActorSystem@node2:2552
- Member is Up: akka://ActorSystem@node2:2552
```
Akka Event Sourcing Actors uses a database to store events and snapshots, 
during application startup we will see in the log
```
- Database initialized successfully
```
If the application failed to connect to the database server, we will see the corresponding message and reason in the log 
```
- An error occurred while initializing the database: ...
```

### Test manualy
You can call GRPC API methods manualy with grpcurl util.
By default, API port is 8080.
```bash
grpcurl \ 
-d '{"player1": {"id": "4e0453ee-c7ba-11ec-9d64-0242ac120002"}, "player2": {"id": "541042b6-c7ba-11ec-9d64-0242ac120002"}}' 
-plaintext \
-import-path /home/capdev/Projects/sea-battle/src/main/protobuf \
-proto battle.proto \
localhost:8080 battle.BattleService/start
```

### Add node
if you want more secondary nodes with shards, you need add :

```yaml
  node3:
    build: .
    environment:
      - ARTERY_HOST=node3
      - SEED1_HOST=node1
      - SEED1_PORT=2551
      - SEED2_HOST=node2
      - SEED2_PORT=2552
      - DB_HOST=mysql
```