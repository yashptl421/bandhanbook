# ---------- Stage 1: Build the JAR ----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ---------- Stage 2: Run the JAR ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082
ENV PORT=8082

CMD ["sh", "-c", "java -jar bandhanbook-0.0.1-SNAPSHOT.jar --server.port=$PORT"]