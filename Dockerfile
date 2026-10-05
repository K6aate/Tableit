# --- BUILD STAGE ---
FROM gradle:8.14.2-jdk21 AS builder

WORKDIR /app
COPY . .


RUN gradle build -x test

# --- RUNTIME STAGE ---
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
