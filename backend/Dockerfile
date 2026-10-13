FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean verify -DskipTests

FROM eclipse-temurin:21-jdk-alpine AS jdk21

FROM alpine:3.20
WORKDIR /app

COPY --from=jdk21 /opt/java/openjdk /opt/java/jdk21
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_HOME=/opt/java/jdk21
ENV PATH="$JAVA_HOME/bin:$PATH"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]