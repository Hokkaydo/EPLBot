./gradlew shadowJar

docker build -t eplbot:latest --target runtime ./
docker build -t c-runner:latest      -f src/main/java/com/github/hokkaydo/eplbot/module/code/c/Dockerfile      .
docker build -t java-runner:latest   -f src/main/java/com/github/hokkaydo/eplbot/module/code/java/Dockerfile   .
docker build -t python-runner:latest -f src/main/java/com/github/hokkaydo/eplbot/module/code/python/Dockerfile .

docker image tag eplbot:latest        hokkaydo/eplbot:latest
docker image tag c-runner:latest      hokkaydo/c-runner:latest
docker image tag java-runner:latest   hokkaydo/java-runner:latest
docker image tag python-runner:latest hokkaydo/python-runner:latest

docker image push hokkaydo/eplbot:latest
docker image push hokkaydo/c-runner:latest
docker image push hokkaydo/java-runner:latest
docker image push hokkaydo/python-runner:latest
