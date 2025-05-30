FROM gradle:8.14 AS build

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

FROM eclipse-temurin:24-jre AS base

LABEL authors="hokkaydo"
RUN mkdir -p /home/eplbot/persistence && apt-get update && apt-get install -y docker.io && apt-get clean
COPY --from=build /home/gradle/build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar

FROM base AS profiler

RUN apt-get update && apt-get install -y unzip && apt-get clean && \
    wget https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2024.9-docker.zip -P /tmp && \
    unzip /tmp/YourKit-JavaProfiler-2024.9-docker.zip -d /usr/local && \
    rm /tmp/YourKit-JavaProfiler-2024.9-docker.zip
EXPOSE 10001
WORKDIR /home/eplbot
ENTRYPOINT ["java", "-agentpath:/usr/local/YourKit-JavaProfiler-2024.9/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all", "-jar", "eplbot.jar"]

FROM base AS production

WORKDIR /home/eplbot
ENTRYPOINT ["java", "--enable-preview", "-jar", "eplbot.jar"]

FROM eclipse-temurin:24-jre AS local-build

RUN mkdir -p /home/eplbot/persistence && apt-get update && apt-get install -y docker.io && apt-get clean
COPY build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar

WORKDIR /home/eplbot
ENTRYPOINT ["java", "--enable-preview", "-jar", "eplbot.jar"]