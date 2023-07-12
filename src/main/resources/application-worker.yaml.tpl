moj:
  server:
    mode: worker
    controller-endpoint: "controller endpoint format: http(s)://<hostname>:<port>"
    languages:
      java-versions:
        - version: <version number>
          name: <version-name>
          compiler: <path/to/javac>
          runtime: <path/to/java>
spring:
  artemis:
    embedded:
      enabled: false
    mode: native
    broker-url: "broker url format: tcp://<hostname>:61616"
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
server:
  port: 8081