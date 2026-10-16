# syntax=docker/dockerfile:1
# Multi-stage image for platform-app. Build: docker build -t recruiting-platform .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY contracts/pom.xml contracts/pom.xml
COPY security/pom.xml security/pom.xml
COPY privacy/pom.xml privacy/pom.xml
COPY crm-accounts/pom.xml crm-accounts/pom.xml
COPY crm-pipeline/pom.xml crm-pipeline/pom.xml
COPY ai/pom.xml ai/pom.xml
COPY platform-app/pom.xml platform-app/pom.xml
COPY contracts contracts
COPY security security
COPY privacy privacy
COPY crm-accounts crm-accounts
COPY crm-pipeline crm-pipeline
COPY ai ai
COPY platform-app platform-app
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl platform-app -am package -DskipTests \
    && cp platform-app/target/platform-app-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination /workspace/extracted

FROM eclipse-temurin:21-jre AS runtime
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
