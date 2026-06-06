# ---------------------------
# Build stage
# ---------------------------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /home/gradle/app

COPY gradle/ gradle/
COPY settings.gradle build.gradle gradlew ./

# Optional but speeds up dependency caching
RUN ./gradlew dependencies --no-daemon || true

COPY src/ src/

RUN ./gradlew shadowJar --no-daemon

# ---------------------------
# Runtime stage
# ---------------------------
FROM eclipse-temurin:25-jre AS runtime

WORKDIR /home/eplbot

RUN mkdir -p persistence

COPY --from=build /home/gradle/app/build/libs/*-all.jar app.jar

ENTRYPOINT ["java", "--enable-preview", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]