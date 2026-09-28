# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Copy the POM first so dependency resolution caches independently of source changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/watchdog-api-*.jar app.jar
# Render's free tier is 512MB; cap the heap so the JVM doesn't get OOM-killed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
