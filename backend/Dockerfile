# ─── Stage 1: Build ─────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and POM first (for layer caching)
COPY pom.xml .
COPY .mvn/ .mvn/ 2>/dev/null || true
COPY mvnw . 2>/dev/null || true

# Download dependencies (cached layer)
RUN if [ -f mvnw ]; then ./mvnw dependency:go-offline -B; \
    else apk add --no-cache maven && mvn dependency:go-offline -B; fi

# Copy source
COPY src/ src/

# Build
RUN if [ -f mvnw ]; then ./mvnw clean package -DskipTests -B; \
    else mvn clean package -DskipTests -B; fi

# ─── Stage 2: Runtime ───────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
