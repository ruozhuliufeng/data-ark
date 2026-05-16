FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm config set registry https://registry.npmjs.org/ \
    && npm config set fetch-retries 5 \
    && npm config set fetch-retry-mintimeout 20000 \
    && npm config set fetch-retry-maxtimeout 120000 \
    && npm ci
COPY frontend ./
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
COPY frontend/package*.json ./frontend/
COPY --from=frontend-build /frontend/dist ./frontend/dist
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       gzip ca-certificates default-mysql-client postgresql-client \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/data-ark-0.1.0-SNAPSHOT.jar /app/data-ark.jar

ENV DATAARK_PORT=7001 \
    DATAARK_CONFIG_FILE=/app/config/dataark.properties \
    DATAARK_DATA_DIR=/app/data \
    DATAARK_WORK_DIR=/app/work \
    DATAARK_BACKUP_DIR=/app/backup \
    DATAARK_LOG_DIR=/app/logs \
    DATAARK_BACKUP_CONCURRENCY=1 \
    DATAARK_GZIP_LEVEL=1 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=65 -XX:ActiveProcessorCount=2"

VOLUME ["/app/config", "/app/data", "/app/work", "/app/backup", "/app/logs"]
EXPOSE 7001

ENTRYPOINT ["java", "-jar", "/app/data-ark.jar"]
