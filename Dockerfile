FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B clean verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S game && adduser -S game -G game
WORKDIR /app
COPY --from=build /app/target/animal-rescue-maze-backend-1.0.0.jar app.jar
USER game
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
