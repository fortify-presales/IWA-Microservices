FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY gradle gradle
COPY plugins plugins
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY libs libs
COPY services/prescriptions services/prescriptions

RUN chmod +x ./gradlew
RUN ./gradlew :services:prescriptions:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/app/services/prescriptions/build/libs/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
