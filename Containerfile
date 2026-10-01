# syntax=docker/dockerfile:1
#
# One image for both ERT processes; pick one with the container args:
#   com.hedera.exchange.server.ExchangeRateApiServer   (default) GET /latest, GET /history
#   com.hedera.exchange.server.ExchangeRateToolService hourly exchange rate update job
#
#   docker buildx build --platform linux/amd64 -t hedera-exchange-rate-tool:local --load .

# Bytecode is platform independent, so compile natively on the build host even when targeting another arch.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home ert
WORKDIR /app
COPY --from=build /workspace/target/hedera-exchange-rate-tool.jar app.jar
USER 10001
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-cp", "/app/app.jar"]
CMD ["com.hedera.exchange.server.ExchangeRateApiServer"]
