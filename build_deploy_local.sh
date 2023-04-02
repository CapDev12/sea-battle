sbt assembly

docker build -t beelink1:5000/sea-battle:latest .
docker push beelink1:5000/sea-battle:latest

docker compose up