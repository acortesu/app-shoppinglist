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

RUN ls -lah /app/ || echo "ERROR: /app directory missing"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "set -x; echo 'Starting JVM...' >&2; ls -la /app/app.jar >&2; java ${JAVA_OPTS} -jar /app/app.jar 2>&1"]
