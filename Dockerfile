FROM ghcr.io/graalvm/native-image-community:25 AS builder

WORKDIR /build

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle/

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src/

RUN ./gradlew nativeCompile --no-daemon

FROM gcr.io/distroless/base-debian12:nonroot

WORKDIR /app

COPY --from=builder --chown=nonroot:nonroot /build/build/native/nativeCompile/* /app/server

EXPOSE 8084

ENTRYPOINT ["/app/server"]