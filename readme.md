


## moj assignment server

De applicatie is een spring Boot Web MVC met Thymeleaf, H2 database, Spring Integration en Websockets

- starten in Eclipse via Run as > Spring Boot App
- commandline: java - jar server-0.0.1-SNAPSHOT.jar

### Gebruik

#### Start opgave
- open http://localhost:8080/control
- login met control/control
- als er opgaven staan, selecteer er 1 en klik 'start Task'



#### Maak opgave
- open http://localhost:8080/
- login met team1/team1 (t/m team40)
- zodra door *control* op *start Task* geklikt is, verschijnt de opgave

#### Download assignment repo
- check application.yaml
- voeg eventueel je ssh public key toe aan de Bitbucket assignment repo
- open http://localhost:8080/control
- click *Clone assignments repo*