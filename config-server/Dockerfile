FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform-common/pom.xml platform-common/pom.xml
COPY config-server/pom.xml config-server/pom.xml
RUN mvn -q -pl config-server -am dependency:go-offline
COPY platform-common platform-common
COPY config-server config-server
RUN mvn -q -pl config-server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server-1.0.0.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","/app/app.jar"]
