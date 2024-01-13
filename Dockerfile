FROM eclipse-temurin:21-jre
LABEL authors="hokkaydo"

RUN mkdir -p /home/eplbot/persistence
COPY build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar
COPY variables.env /home/eplbot/variables.env
WORKDIR /home/eplbot
ENTRYPOINT ["java", "--enable-preview", "-jar", "eplbot.jar"]