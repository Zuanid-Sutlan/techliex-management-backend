# Stage 1: Build stage using Gradle & JDK 21
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle configuration & wrapper
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

RUN chmod +x gradlew

# Copy application source code
COPY src src

# Build executable fat JAR using container's JDK 21
RUN ./gradlew build -x test --no-daemon -Porg.gradle.java.installations.auto-download=false

# Stage 2: Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads

# Copy compiled JAR artifact
COPY --from=build /app/build/libs/*.jar /app/techliex-management.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/techliex-management.jar"]
