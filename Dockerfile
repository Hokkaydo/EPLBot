FROM eclipse-temurin:19-jdk-jammy
RUN apt-get update -y
RUN apt-get install -y python3 rustc
RUN ln -s /usr/bin/python3 /usr/bin/python
RUN apt-get clean #remove cache of downloaded things. 
LABEL authors="hokkaydo"
RUN mkdir -p /home/eplbot/persistence
COPY build/libs/EPLBot-1.0-SNAPSHOT-all.jar /home/eplbot/eplbot.jar
COPY variables.env /home/eplbot/variables.env
WORKDIR /home/eplbot
ENTRYPOINT ["java", "-jar", "eplbot.jar"]