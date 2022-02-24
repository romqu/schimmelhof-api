FROM gradle:7.2.0-jdk16 as cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle.kts /home/gradle/java-code/
COPY settings.gradle.kts /home/gradle/java-code/
WORKDIR /home/gradle/java-code
RUN gradle clean build -i --stacktrace -x bootJar

FROM gradle:7.2.0-jdk16 as builder
ARG ENVPROFILE
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY . /usr/src/java-code/
WORKDIR /usr/src/java-code
RUN  gradle assemble "-Dorg.gradle.jvmargs=--illegal-access=permit -Dspring.profiles.active=$ENVPROFILE"

FROM adoptopenjdk/openjdk16:alpine-jre
ARG ENVPROFILE
ENV ENVPROFILE_ENV=$ENVPROFILE
EXPOSE 8080
USER root
WORKDIR /usr/src/java-app
COPY --from=builder /usr/src/java-code/build/libs/*.jar app.jar
ENTRYPOINT java -Dspring.profiles.active=$ENVPROFILE_ENV -jar app.jar