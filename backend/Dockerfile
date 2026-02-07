FROM gradle:8.10-jdk17 AS build
WORKDIR /home/gradle/project

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle --no-daemon bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
