FROM openjdk:11
COPY target/server-0.0.1-SNAPSHOT.jar /opt/moj/server.jar
ENTRYPOINT ["java","-jar","/opt/moj/server.jar"]
VOLUME /tmp/assignments
VOLUME /opt/mojserver/db/test
EXPOSE 8080