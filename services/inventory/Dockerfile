FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY gradle gradle
COPY plugins plugins
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY libs libs
COPY services/inventory services/inventory

RUN chmod +x ./gradlew
RUN ./gradlew :services:inventory:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/app/services/inventory/build/libs/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
