# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Pre-download dependencies to cache this step
RUN mvn dependency:go-offline -B
COPY src ./src
# Build package with production profile and skip tests
RUN mvn clean package -Pproduction -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/rag-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 7860
ENTRYPOINT ["java", "-jar", "app.jar"]
