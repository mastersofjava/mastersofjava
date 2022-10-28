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

Use the following steps to get Keycloak up and running.

- Download the latest standalone keycloak 19 from the link on the page [here](https://www.keycloak.org/getting-started/getting-started-zip)
- Unpack the installation file to a directory
- Copy the `src/main/keycloak-template/moj` directory from this project to `themes` directory within the keycloak 
  installation directory 
- Run it using `bin/kc.sh --hostname localhost --hostname-strict-https false --http-enabled true --http-port 8888`
  (or for windows users: `bin/kc.bat start --hostname localhost --hostname-strict-https false --http-enabled true --http-port 8888`)
- Go the webpage at http://localhost:8888
   * Create an admin account and log in
   * Open the top left dropdown and select 'Add realm'
     * Click on 'Browse' and select the file `keycloak-19-moj-realm.json` in the root of this project
     * Click 'Create'
   * Click on 'Realm settings' in the menu bar on the left. 
     * Click on 'Partial import' from the top right 'Action' dropdown.
     * Click on 'Browse' and select the file `keycloak-19-moj-realm.json` in the root of this project again
     * Select all checkboxes under 'Choose the resources you want to import'
     * Select 'Skip' as resolution for resources already existing
     * Click 'Import' and then 'Close'
     * Due to https://github.com/keycloak/keycloak/issues/12256 the following steps need to be done also.
       * Update the 'Display name' to 'Masters of Java' (see )
       * Click the 'Login' tab and enable 'User registration'.
       * Click the 'User registration tab' and then the tab 'Default groups'
         * Click 'Add groups'
         * Click `>` behind the `moj` group
         * Select `users` and click 'Add'
       * Click the 'General' tab and then 'Save'
  * Click on 'Users' in the menu bar on the left.
    * Click on 'Create new user' and fill in the following form values:
      * Username: `admin`
      * Email: <a valid email address>
      * Email verified: toggle to 'On'
      * Firstname: 'A'
      * Lastname: 'Admin'
      * Enabled: toggle to 'On'
      * Required user actions: leave blank
      * Click 'Join Groups'
        * Click `>` behind the `moj` group
        * Select `admins`
        * Click 'Join' and then 'Create'
      * Click on the 'Credentials' tab and then on 'Set password'
        * Fill in the two password fields with the same password
        * Toggle 'Temporary' to off
        * Click 'Save' and then 'Save password'
   
You should now have Keycloak running with a single admin user.
            
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


