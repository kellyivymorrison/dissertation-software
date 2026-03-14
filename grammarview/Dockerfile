# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
# The directory where the user will mount their data
WORKDIR /app

# Copy the JAR to /opt so it isn't hidden when the user mounts a volume to /app
COPY --from=build /app/target/grammarview-1.0-SNAPSHOT.jar /opt/grammarview.jar

# Define an entrypoint so the container acts like a CLI tool
ENTRYPOINT ["java", "-jar", "/opt/grammarview.jar"]
