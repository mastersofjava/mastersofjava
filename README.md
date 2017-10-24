# Masters of Java #

This project is a rebuild of the famous original Masters of Java software orginally built by Erik Hooijmeijer in 2004. 
This project takes the original concept, but instead of a classical client/server Swing application, it is a completely 
web-based implementation based on Spring Boot and Java 9.

## moj assignment server

De applicatie is een spring Boot Web MVC met Thymeleaf, H2 database, Spring Integration en Websockets

- starten in Eclipse via Run as > Spring Boot App
- commandline: java - jar server-0.0.1-SNAPSHOT.jar

### Voorbereiding

- pas in de application.yaml de property 'basedir' aan naar een pad op je lokale systeem, pas ook de 'datasource.url' property aan.
- maak een lib directory in de basedir, zet daar deze 2 jars in:
- wget http://central.maven.org/maven2/junit/junit/4.12/junit-4.12.jar
- wget http://central.maven.org/maven2/org/hamcrest/hamcrest-all/1.3/hamcrest-all-1.3.jar


### Gebruik

#### Start opgave
- open http://localhost:8080/control
- login met control/control
- als er opgaven staan, selecteer er 1 en klik 'start Task'
- anders klik *Clone assignments repo*


#### Maak opgave
- open http://localhost:8080/
- login met team1/team1 (t/m team40)
- zodra door *control* op *start Task* geklikt is, verschijnt de opgave

#### Download assignment repo
- check application.yaml
- voeg eventueel je ssh public key toe aan de Bitbucket assignment repo
- open http://localhost:8080/control
- click *Clone assignments repo* 

#### Ranking pagina

- open http://localhost:8080/rankings

#### Feedback pagina
- open http://localhost:8080/feedback
