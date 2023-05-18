FROM eclipse-temurin:19-jdk-jammy
LABEL authors="hokkaydo"

CMD ["gradlew", "shadowJar"]
COPY build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar
WORKDIR /home/eplbot
ENTRYPOINT ["java", "-jar", "eplbot.jar"]
