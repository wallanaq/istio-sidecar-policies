# ─── Stage 1: build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY apps/bin-checker-service/pom.xml apps/bin-checker-service/
COPY apps/bin-checker-service/src     apps/bin-checker-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f apps/bin-checker-service/pom.xml package -B -DskipTests

# ─── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/apps/bin-checker-service/target/*.jar app.jar

RUN chown app:app app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
