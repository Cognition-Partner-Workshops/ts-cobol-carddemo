# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom files first for better caching
COPY pom.xml .
COPY carddemo-domain/pom.xml carddemo-domain/
COPY carddemo-persistence/pom.xml carddemo-persistence/
COPY carddemo-services/pom.xml carddemo-services/
COPY carddemo-api/pom.xml carddemo-api/
COPY carddemo-batch/pom.xml carddemo-batch/
COPY carddemo-migration/pom.xml carddemo-migration/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY carddemo-domain/src carddemo-domain/src
COPY carddemo-persistence/src carddemo-persistence/src
COPY carddemo-services/src carddemo-services/src
COPY carddemo-api/src carddemo-api/src
COPY carddemo-batch/src carddemo-batch/src
COPY carddemo-migration/src carddemo-migration/src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN groupadd -r carddemo && useradd -r -g carddemo carddemo

# Copy the built JAR from build stage
COPY --from=build /app/carddemo-api/target/*.jar app.jar

# Set ownership
RUN chown -R carddemo:carddemo /app

# Switch to non-root user
USER carddemo

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
