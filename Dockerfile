# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

COPY --from=build /workspace/target/*-runner.jar /app/app.jar
COPY OnlineBot_Config.json /app/OnlineBot_Config.json

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=${PORT} -jar /app/app.jar"]
