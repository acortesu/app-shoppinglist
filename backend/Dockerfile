FROM gradle:8.10-jdk17 AS build
WORKDIR /home/gradle/project

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle --no-daemon bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar

# JVM startup optimizations for Cloud Run cold start
# -XX:+UseSerialGC: minimize memory footprint
# -Xshare:auto: enable class data sharing if available
# -XX:TieredStopAtLevel=1: skip higher tier compilation, use interpreter
# -Djava.security.egd: use /dev/urandom for entropy
ENV JAVA_OPTS="-XX:+UseSerialGC -Xshare:auto -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
ENTRYPOINT exec java ${JAVA_OPTS} -Dlogging.level.root=INFO -jar /app/app.jar
