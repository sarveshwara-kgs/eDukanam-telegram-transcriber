# Multi-stage build for Spring Boot 3.3.4 / Java 17
# Image: eDukanam-telegram-transcriber:latest

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY ./pom.xml ./
COPY ./src ./src
RUN mvn clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/telegram-transcription-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "./app.jar"]
