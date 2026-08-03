FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl api-gateway -am clean package -DskipTests
FROM eclipse-temurin:21-jre
COPY --from=build /workspace/api-gateway/target/api-gateway-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
