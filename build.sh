docker build -t eplbot --target runtime ./
docker build -t c-runner      -f src/main/java/com/github/hokkaydo/eplbot/module/code/c/Dockerfile      .
docker build -t java-runner   -f src/main/java/com/github/hokkaydo/eplbot/module/code/java/Dockerfile   .
docker build -t python-runner -f src/main/java/com/github/hokkaydo/eplbot/module/code/python/Dockerfile .
docker compose -f docker-compose-local.yml up eplbot --remove-orphans --force-recreate
