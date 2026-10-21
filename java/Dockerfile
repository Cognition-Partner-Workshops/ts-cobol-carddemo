# Build the jar first: (cd java && mvn -q -B package -DskipTests)
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre
WORKDIR /app
COPY target/carddemo-batch-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
