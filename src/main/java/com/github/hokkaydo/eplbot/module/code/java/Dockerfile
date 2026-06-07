FROM eclipse-temurin:25-jdk-alpine
WORKDIR /usr/src/app/logs
CMD ["sh", "-c", "cat > /usr/src/app/logs/Main.java && javac -d /usr/src/app/logs /usr/src/app/logs/Main.java && java -cp /usr/src/app/logs Main"]
