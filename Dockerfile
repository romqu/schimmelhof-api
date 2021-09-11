FROM gradle:7.2.0-jdk16 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble --no-daemon

FROM openjdk:16

EXPOSE 8080

RUN mkdir /app

COPY --from=build "/home/gradle/src/build/libs/schimmelhof-api-0.0.1-SNAPSHOT.jar" /app/application.jar

CMD ["java", "-jar", "/app/application.jar"]