FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

ARG APP_VERSION=0.0.0

COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test -PappVersion=$APP_VERSION

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
