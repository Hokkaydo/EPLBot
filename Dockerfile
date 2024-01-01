FROM eclipse-temurin:19-jdk-jammy
LABEL authors="hokkaydo"
ADD . /
RUN wget --no-verbose https://github.com/asciinema/agg/releases/download/v1.4.3/agg-x86_64-unknown-linux-musl -O /usr/bin/agg
RUN chmod +x /usr/bin/agg
RUN /gradlew shadowJar
RUN mkdir /persistence
ENTRYPOINT ["/gradlew","run"]
