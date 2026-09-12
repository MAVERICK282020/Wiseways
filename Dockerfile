# ── Build stage ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# ── Runtime stage ────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# CSV datasets are loaded from the working directory at runtime
COPY JEE_Rank_2016_2024.csv uptac2.csv ./
COPY --from=build /app/target/machine-engine-1.0.0.jar app.jar

EXPOSE 5000
# NVIDIA_API_KEY is injected via environment (never baked into the image)
ENTRYPOINT ["java", "-jar", "app.jar"]
