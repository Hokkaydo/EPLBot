./gradlew shadowJar
docker build -t eplbot ./
docker image tag eplbot:latest hokkaydo/eplbot:latest
docker image push hokkaydo/eplbot:latest