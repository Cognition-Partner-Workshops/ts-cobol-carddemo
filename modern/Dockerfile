# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY modern/ modern/
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn -B -f modern/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /estate
COPY --from=build /src/modern/interest/target/interest-0.1.0.jar /opt/carddemo/interest.jar
COPY --from=build /src/modern/trantype-update/target/trantype-update-0.1.0.jar /opt/carddemo/trantype-update.jar
COPY --from=build /src/modern/trantype-dynamic/target/trantype-dynamic-0.1.0.jar /opt/carddemo/trantype-dynamic.jar
COPY --from=build /src/modern/posting/target/posting-0.1.0.jar /opt/carddemo/posting.jar
COPY modern/run.py /opt/carddemo/run.py
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
ENTRYPOINT ["python3", "/opt/carddemo/run.py"]
