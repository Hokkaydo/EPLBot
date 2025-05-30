./gradlew shadowJar
docker build -t eplbot:latest --target local-build ./
#docker build -t eplbot:latest --target production ./
# docker build -t eplbot-profiler:latest --target profiler ./
docker image tag eplbot:latest hokkaydo/eplbot:latest
# docker image tag eplbot-profiler:latest hokkaydo/eplbot-profiler:latest
# sh build_code_docker.sh
docker image push hokkaydo/eplbot:latest
# docker image push hokkaydo/eplbot-profiler:latest
# docker image push hokkaydo/java-runner:latest
# docker image push hokkaydo/c-runner:latest
# docker image push hokkaydo/python-runner:latest
