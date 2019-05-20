# Masters of Java

This project is a rebuild of the famous original Masters of Java software orginally built by Erik Hooijmeijer in 2004. 
This project takes the original concept, but instead of a classical client/server Swing application, it is a completely 
web-based implementation.

## moj game server

The game server uses Spring Boot, Thymeleaf, H2 database and Websockets

### Requirements

- Java 11 of hoger
- Maven 3.5 of hoger voor builden van source.

### Preparation

Make sure you update the application.yaml to it works for the machine that is running the game server.  

#### Starting
From any IDE you can just run the MojServerApplication.class

#### Controller Dashboard

The controller dashboard can be found on [http://localhost:8080/control](http://localhost:8080/control). The login is 
`control` the password is randomly generated every time you start the server. It is printed in the logs on the console.

TODO add commandline start info
TODO add info on external configuration info