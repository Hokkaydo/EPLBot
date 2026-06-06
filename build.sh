./gradlew shadowJar
docker build -t eplbot --target runtime ./
docker compose -f docker-compose-local.yml up eplbot --remove-orphans --force-recreate