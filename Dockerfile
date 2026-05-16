FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
COPY frontend ./frontend
COPY --from=frontend-build /frontend/dist ./frontend/dist
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       gzip ca-certificates rclone default-mysql-client postgresql-client \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/data-ark-0.1.0-SNAPSHOT.jar /app/data-ark.jar

ENV DATAARK_PORT=8080 \
    DATAARK_CONFIG_FILE=/app/config/dataark.properties \
    DATAARK_DATA_DIR=/app/data \
    DATAARK_WORK_DIR=/app/work \
    DATAARK_BACKUP_DIR=/app/backup \
    DATAARK_LOG_DIR=/app/logs

VOLUME ["/app/config", "/app/data", "/app/work", "/app/backup", "/app/logs", "/root/.config/rclone"]
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/data-ark.jar"]
