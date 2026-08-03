FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl notification-service package -DskipTests
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/notification-service/target/notification-service-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
