FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl user-service -am package -DskipTests
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/user-service/target/user-service-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
