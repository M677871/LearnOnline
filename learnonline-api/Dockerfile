FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Build the jar on host first: mvnw.cmd clean package -DskipTests
COPY target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]