FROM registry.cloud.bliep.net/moj/base-jdk11:latest

MAINTAINER First8 B.V.

VOLUME [ "/data" ]
EXPOSE 8080
EXPOSE 5000

COPY server-0.0.1-SNAPSHOT.jar /app/app.jar

CMD ["-jar", "/app/app.jar", "--spring.profiles.active=docker" ]
