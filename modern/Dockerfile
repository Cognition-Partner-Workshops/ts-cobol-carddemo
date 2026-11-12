# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
# An optional local settings secret supports restricted networks without baking settings into the image.
RUN --mount=type=secret,id=maven_settings \
    if [ -f /run/secrets/maven_settings ]; then \
      mvn -B -s /run/secrets/maven_settings package -DskipTests; \
    else mvn -B package -DskipTests; fi

FROM eclipse-temurin:21-jre
WORKDIR /estate
COPY --from=build /build/interest/target/interest-0.1.0.jar /opt/carddemo/interest.jar
COPY --from=build /build/posting/target/posting-0.1.0.jar /opt/carddemo/posting.jar
COPY --from=build /build/trantype-update/target/trantype-update-0.1.0.jar /opt/carddemo/trantype-update.jar
COPY --from=build /build/trantype-dynamic/target/trantype-dynamic-0.1.0.jar /opt/carddemo/trantype-dynamic.jar
COPY run-job /opt/carddemo/run-job
ENTRYPOINT ["/opt/carddemo/run-job"]
