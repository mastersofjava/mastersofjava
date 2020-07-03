# Masters of Java

This project is a rebuild of the famous original Masters of Java software orginally built by Erik Hooijmeijer 
in 2004 (see [http://www.ctrl-alt-dev.nl/mastersofjava/]. This project takes the original concept, but instead 
of a classical client/server Swing application, it is a completely web-based re-implementation.

## moj game server

The game server uses Spring Boot,Keycloak, Thymeleaf, H2 database and Websockets.

### Requirements

- Java 11+
- Maven 3.5+
- Keycloak

### Preparation

Make sure you update the application.yaml to it works for the machine that is running the game server.  
- The resources are default located in: ./moj-data/
- For downloading all javadocs via commandline: `mvn dependency:sources dependency:resolve -Dclassifier=javadoc`
- Download the newest SDK documentation and extract (directory `api`) into directory: `./moj-data/javadoc/api` (now the participants have easily access to these docs via the GUI))
- Don't forget to configure your Keycloak instance with the right realm, roles and users and use this in the keycloak configuration in the game server

#### Starting
- From any IDE you can just run the `MojServerApplication.class`
- Startup via commandline: `mvn compile spring-boot:run`
- Advice: do not change the official application.yaml when not needed. 
    - Use a customizable application-local.yaml via: `mvn compile spring-boot:run -Dspring.profiles.active=local`

#### Gamemaster Dashboard

The Gamemaster dashboard can be found on [http://localhost:8080/control](http://localhost:8080/control). The first time
you start the application you will be asked to setup. 


# License

   Copyright 2020 First Eight BV (The Netherlands)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


