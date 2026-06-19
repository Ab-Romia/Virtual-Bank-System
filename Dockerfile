# One Dockerfile builds any service module. docker-compose passes MODULE per service.
# A reactor build compiles the parent, the shared library, and the target module
# together, so a clone plus "docker compose up --build" needs nothing on the host.

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY pom.xml ./
COPY vbank-common vbank-common
COPY gateway gateway
COPY user-service user-service
COPY account-service account-service
COPY transaction-service transaction-service
COPY audit-service audit-service
RUN mvn -q -B -ntp -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
