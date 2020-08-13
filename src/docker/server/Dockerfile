FROM adoptopenjdk/openjdk11:x86_64-ubi-minimal-jdk-11.0.8_10

MAINTAINER First8 B.V.

ENV DOCKERIZE_OPTS ""
ENV DOCKERIZE_VERSION v0.6.1
RUN curl -OL https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

VOLUME [ "/app" ]
VOLUME [ "/data" ]

WORKDIR /app

EXPOSE 8080

COPY server-0.0.1-SNAPSHOT.jar /app/app.jar
COPY ./entry.sh /
ENTRYPOINT [ "/entry.sh" ]

CMD ["-jar", "/app/app.jar", "--spring.profiles.active=docker" ]
