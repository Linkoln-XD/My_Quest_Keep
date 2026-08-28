FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S questkeep && adduser -S questkeep -G questkeep
COPY --from=build /workspace/target/questkeep-0.0.1-SNAPSHOT.jar app.jar
USER questkeep
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
