# syntax=docker/dockerfile:1

# ---------------------------
# Build stage
# ---------------------------
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /home/gradle/app

COPY gradle/ gradle/
COPY settings.gradle build.gradle gradlew ./
COPY src/ src/

# BuildKit cache mount keeps the Gradle dependency cache across image rebuilds
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew shadowJar --no-daemon

# ---------------------------
# Runtime stage
# ---------------------------
FROM eclipse-temurin:25-jre AS runtime

WORKDIR /home/eplbot

COPY --from=build /home/gradle/app/build/libs/*-all.jar app.jar

COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker

# rclone lists the EPL Drive for the contributions module, its config (and OneDrive token) lives in the persistence volume
COPY --from=rclone/rclone:1 /usr/local/bin/rclone /usr/local/bin/rclone
ENV RCLONE_CONFIG=/home/eplbot/persistence/rclone.conf

ENTRYPOINT ["java", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
