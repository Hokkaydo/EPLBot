FROM gradle:jdk25 AS build

ENV HOME=/home/gradle
RUN mkdir -p "$HOME"/.gradle
WORKDIR $HOME

# Copy gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/

# Download dependencies
RUN ./gradlew --no-daemon dependencies

# Copy source code
COPY src/ src/

# Build app
RUN ./gradlew shadowJar

FROM eclipse-temurin:25-jre AS base

LABEL authors="hokkaydo"
RUN mkdir -p /home/eplbot/persistence && apt-get update && apt-get install -y docker.io && apt-get clean
COPY --from=build /home/gradle/build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar

FROM base AS production

WORKDIR /home/eplbot
ENTRYPOINT ["java", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-jar", "eplbot.jar"]

FROM eclipse-temurin:25-jre AS local-build

RUN mkdir -p /home/eplbot/persistence && apt-get update && apt-get install -y docker.io && apt-get clean
COPY build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar

WORKDIR /home/eplbot
ENTRYPOINT ["java", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-jar", "eplbot.jar"]