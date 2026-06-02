# ── Stage 1: Build ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Download dependencies first (layer cache optimization)
RUN apk add --no-cache maven \
 && mvn dependency:go-offline -q

# Build the jar (skip tests in Docker build; run tests separately in CI)
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /app/target/order-service-1.0.0.jar app.jar

EXPOSE 8080

# Profile can be overridden at runtime: docker run -e SPRING_PROFILES_ACTIVE=prod
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
