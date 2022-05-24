FROM openjdk:11-jre-slim

COPY target/scala-2.13/sea-battle.jar sea-battle.jar

ENTRYPOINT ["java","-jar","sea-battle.jar"]
