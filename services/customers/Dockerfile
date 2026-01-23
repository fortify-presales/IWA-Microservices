FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY libs libs
COPY services/customers services/customers

RUN ./gradlew :services:customers:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/app/services/customers/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
