FROM eclipse-temurin:19-jdk-jammy
LABEL authors="hokkaydo"

COPY build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar
COPY variables.env /home/eplbot/variables.env
WORKDIR /home/eplbot
ENTRYPOINT ["java", "-jar", "eplbot.jar"]