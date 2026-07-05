FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q
COPY src src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", \
  "-XX:+UseZGC", "-XX:+ZGenerational", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseStringDeduplication", \
  "-Xss256k", \
  "-jar", "app.jar"]
