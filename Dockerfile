FROM eclipse-temurin:19-jdk-jammy
LABEL authors="hokkaydo"
ADD . /
RUN /gradlew shadowJar
RUN mkdir /persistence
ENTRYPOINT ["/gradlew","run"]
