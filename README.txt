#docker start sea-battle-mysql

docker run \
--detach \
--name=sea-battle-mysql \
--env="MYSQL_ROOT_PASSWORD=password" \
--publish 3306:3306 \
--volume=/home/capdev/mysql/data:/var/lib/mysql \
mysql:latest

#--volume=~/home/capdev/mysql/conf.d:/etc/mysql/conf.d


node-1
java -DARTERY_PORT=2551 -jar target/scala-2.13/sea-battle.jar

node-2
java -DARTERY_PORT=2552 -DGRPC_PORT=8081 -jar target/scala-2.13/sea-battle.jar

