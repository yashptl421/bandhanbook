# ---------- Stage 1: Build the JAR ----------
# ====== BUILD STAGE ======
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Package without tests
RUN mvn clean package -DskipTests

# ====== RUN STAGE ======
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082
ENV PORT=8082

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]