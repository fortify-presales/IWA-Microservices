FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY libs libs
COPY apps/gateway apps/gateway

RUN ./gradlew :apps:gateway:build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/app/apps/gateway/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
