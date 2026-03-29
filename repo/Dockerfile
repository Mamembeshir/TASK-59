FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package \
    && set -- /workspace/build/*.jar \
    && [ -f "$1" ] \
    && cp "$1" /workspace/build/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p /uploads

COPY --from=build /workspace/build/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
