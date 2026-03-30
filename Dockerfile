FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test && ls -la build/libs/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/ ./build/libs/
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=prod -jar build/libs/*.jar"]
