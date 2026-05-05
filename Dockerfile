# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
# Shell form so Cloud Run's PORT is expanded (exec form does not expand ${PORT})
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -Dspring.profiles.active=supabase -jar app.jar"]
