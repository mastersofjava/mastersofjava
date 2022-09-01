# Masters of Java

This project is a rebuild of the famous original Masters of Java software orginally built by Erik Hooijmeijer 
in 2004 [see http://www.ctrl-alt-dev.nl/mastersofjava/](http://www.ctrl-alt-dev.nl/mastersofjava/). This project takes the original concept, but instead 
of a classical client/server Swing application, it is a completely web-based re-implementation.

Assignments for previous years can be found at [https://github.com/First8/mastersofjava/](https://github.com/First8/mastersofjava/).

## moj game server

The game server uses Spring Boot,Keycloak, Thymeleaf, H2 database and Websockets.

### Requirements

- Java 17+
- Maven 3.5+
- [Keycloak](www.keycloak.org)

### Preparation

Make sure you update the application.yaml to it works for the machine that is running the game server.  
- The resources are default located in: ./moj-data/
- For downloading all javadocs via commandline: `mvn dependency:sources dependency:resolve -Dclassifier=javadoc`
- Download the newest SDK documentation and extract (directory `api`) into directory: `./moj-data/javadoc/api` (now the participants have easily access to these docs via the GUI))
- This project requires a properly configured keycloak to run for user authentication, see below:

#### Keycloak installation, execution and configuration

The procedure below is based on [this tutorial](https://www.baeldung.com/spring-boot-keycloak) but could change based on differences in versions
- Download the latest standalone keycloak-server from the link on the page [here](https://www.keycloak.org/getting-started/getting-started-zip)
- Unpack the installation file to a directory
- Copy the `src/main/keycloak-template/moj` directory from this project to `themes` directory within the keycloak 
  installation directory 
- Run it using `bin/standalone.sh -Djboss.socket.binding.port-offset=100` or `bin/standalone.bat -Djboss.socket.binding.port-offset=100`
- Go the webpage at http://localhost:8180
   * Create an admin account and log in
   * Hover on the word 'Master' at the left-top of the webpage and select 'Add realm'
        * Name the new realm `moj`
        * Click on 'Import' - 'Select file' and select the file `moj-keycloak-realm-export.json` in the root of this project
   * Navigate on the left menu to 'Clients'
        * Click on 'Create'
        * enter Client ID: `moj` and click on save
   * Navigate on the left menu to 'Users' 
        * Click on 'Add user'
        * add 'Username': `admin`, enter a password, and click on save
        * Click on 'View all users'
        * Select on the id of the user with username `admin`
        * click on tab 'Role mappings'
            * add role 'ROLE_ADMIN' and 'ROLE_GAME_MASTER'
            
#### Starting
- From any IDE you can just run the `MojServerApplication.class`
- Startup via commandline: `mvn compile spring-boot:run`
- Advice: do not change the official application.yaml when not needed. 
    - Use a customizable application-local.yaml via: `mvn compile spring-boot:run -Dspring.profiles.active=local`

#### Gamemaster Dashboard

The Gamemaster dashboard can be found on [http://localhost:8080/control](http://localhost:8080/control). The first time 
you start the application you will have to log in using the admin credentials created in the keycloak setup and the 
MoJ game server admin console should appear. 

All following web sessions will also be redirected to keycloak and one can create a new account there and log in. When 
you want to run the admin console and login with a user as well on the same desktop environment, you need to open a 
separate browser session, e.g. using a 'private window' or another browser implementation.


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


