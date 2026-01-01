FROM ghcr.io/graalvm/native-image-community:25 AS builder

WORKDIR /app

RUN dd if=/dev/zero of=/swapfile bs=1M count=2048 status=progress && \
    chmod 600 /swapfile && \
    mkswap /swapfile

RUN swapon /swapfile || true

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew --version

RUN ./gradlew dependencies --no-daemon

COPY src src/
RUN ./gradlew nativeCompile --no-daemon -x test

FROM gcr.io/distroless/base-debian12:nonroot
WORKDIR /app
COPY --from=builder /app/build/native/nativeCompile/chatapps-graph /app/main
EXPOSE 8080
ENTRYPOINT ["/app/main"]