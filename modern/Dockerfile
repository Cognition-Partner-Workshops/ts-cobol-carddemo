ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JAVA_IMAGE=eclipse-temurin:21-jre
FROM ${MAVEN_IMAGE} AS build
WORKDIR /src
COPY modern/ modern/
RUN mvn -B -s modern/maven-settings.xml -f modern/pom.xml verify

FROM ${JAVA_IMAGE}
WORKDIR /estate
COPY --from=build /src/modern/posting/target/posting.jar /opt/carddemo/posting.jar
COPY --from=build /src/modern/interest/target/interest.jar /opt/carddemo/interest.jar
COPY --from=build /src/modern/trantype-update/target/trantype-update.jar /opt/carddemo/trantype-update.jar
COPY --from=build /src/modern/trantype-dynamic/target/trantype-dynamic.jar /opt/carddemo/trantype-dynamic.jar
ENV JAVA_JAR_DIR=/opt/carddemo
CMD ["sleep", "infinity"]
