# Masters of Java #

This project is a rebuild of the famous original Masters of Java software orginally built by Erik Hooijmeijer in 2004. 
This project takes the original concept, but instead of a classical client/server Swing application, it is a completely 
web-based implementation based on Spring Boot and Java 9.

## moj assignment server

De applicatie is een spring Boot Web MVC met Thymeleaf, H2 database, Spring Integration en Websockets

- starten in Eclipse via Run as > Spring Boot App
- commandline: `java - jar server-0.0.1-SNAPSHOT.jar`

### Voorbereiding

- pas in de `application.yaml` de property `moj.server.basedir` aan naar een pad op je lokale systeem, pas ook de `datasource.url` property aan.
- maak een lib directory in de basedir, zet daar deze 2 jars in:
- `wget http://central.maven.org/maven2/junit/junit/4.12/junit-4.12.jar`
- `wget http://central.maven.org/maven2/org/hamcrest/hamcrest-all/1.3/hamcrest-all-1.3.jar`
- pas de `moj.server.javaExecutable` property aan
- kopieer `src/main/resources/securityPolicyForUnitTests.policy` uit het project naar de lib directory

### Gebruik

#### Start applicatie
Run class MojServerApplication

#### Start opgave
- open [http://localhost:8080/control](http://localhost:8080/control)
- login met control/f8!moj20!7
- als er opgaven staan, selecteer er 1 en klik 'start Task'
- anders klik *Clone assignments repo*


#### Maak opgave
- open [http://localhost:8080/](http://localhost:8080/)
- login met team1/team1 (t/m team40)
- zodra door *control* op *start Task* geklikt is, verschijnt de opgave

#### Download assignment repo
- check application.yaml
- voeg eventueel je ssh public key toe aan de Bitbucket assignment repo
- open [http://localhost:8080/control](http://localhost:8080/control)
- click *Clone assignments repo* 

#### Ranking pagina

- open [http://localhost:8080/rankings](http://localhost:8080/rankings)

#### Feedback pagina
- open [http://localhost:8080/feedback](http://localhost:8080/feedback)

## Jmeter test

### Installatie Jmeter
- installeer jmeter
- installeer de jmeter plugin manager: volg instructies op https://jmeter-plugins.org/install/Install/
- Ga naar Options -> Plugins Manager -> Available Plugins
- zoek naar *WebSocket Samplers by Peter Doornbosch*  
- selecteer en *Apply changes...*
- meer info over samplers https://bitbucket.org/pjtr/jmeter-websocket-samplers/overview
- download mjson library naar <apache-jmeter>/lib/ext:  http://repo1.maven.org/maven2/org/sharegov/mjson/1.4.0/mjson-1.4.0.jar
- herstart jmeter

### Test
- creeer test gebruikers met <workspace>/server/createUsers.sh
- open <workspace>/server/src/test/resources/jmeter/*test-mojserver.jmx*
- pas aan filenaam aan in *CSV Data Set Config* naar <workspace>/server/src/test/resources/jmeter/teams.csv
- 
- save
- stop gui
- start mojserver
- run jmeter zonder gui vanuit de <workspace> directory
- <apache-jmeter>/bin/jmeter -n -t server/src/test/resources/jmeter/test-mojserver.jmx
