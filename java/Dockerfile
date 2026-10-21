# Build the jar first: (cd java && mvn -q -B package -DskipTests)
ARG REGISTRY=public.ecr.aws/docker/library
FROM ${REGISTRY}/eclipse-temurin:21-jre
WORKDIR /app
COPY target/carddemo-batch-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
