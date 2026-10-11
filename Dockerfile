# syntax=docker/dockerfile:1

FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace

COPY backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/src ./src

RUN gradle --no-daemon --stacktrace installDist

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=8080
ENV SANDBOX_WORK_DIR=/var/telefam/sandbox/quarantine
ENV SECURE_STORAGE_DIR=/var/telefam/storage/verified_avatars

RUN mkdir -p /var/telefam/sandbox/quarantine /var/telefam/storage/verified_avatars \
    && useradd --system --create-home --uid 10001 telefam \
    && chown -R telefam:telefam /var/telefam /app

COPY --from=build /workspace/build/install/telefam-backend/ ./

USER telefam
EXPOSE 8080

ENTRYPOINT ["/app/bin/telefam-backend"]
