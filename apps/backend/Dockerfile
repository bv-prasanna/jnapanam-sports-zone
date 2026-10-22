# Northflank build context: /apps/backend ; Dockerfile: /apps/backend/Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar
USER spring
ENV SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
