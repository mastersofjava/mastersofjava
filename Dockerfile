FROM openjdk
COPY target/server-0.0.1-SNAPSHOT.jar /opt/moj/server.jar
ENTRYPOINT ["/usr/bin/java"]
CMD ["-jar", "/opt/moj/server.jar"]
VOLUME /tmp/assignments
VOLUME /opt/mojserver/db/test
EXPOSE 8080