./gradlew shadowJar
docker build -t eplbot ./
docker compose -f docker-compose-local.yml up