# Docker Desktop
You can run a full test setup, NOT suitable for production, with the docker-compose.yaml file in `src/deploy/docker-desktop`. 

## Starting

### Hosts filee
Before you start it, make sure you have updated the hosts file to contain the following to make sure OIDC and controller can communicate.

```
127.0.0.1 host.docker.internal 
```

### Starting containers
Once the hosts file is updated, run the following

```shell
$ docker compose up
```
The worker needs to connect to the controller and the controller needs to connect keycloak to start property. This will
give errors during startup, but eventually it will all sort itself out and end up in a running state. Docker compose 
depends_on sadly does not wait for containers to be started.

### Adding users
Once the environment is up, you need to log into the [keycloak console](http://host.docker.internal:8888) with 
username `keycloak` and password `keycloak`. Create a new user in the `moj` realm and add it to the `admin` group.

## GUIs
You can find the game [here](http://localhost:8080) and the game master console [here](http://localhost:8080/control).

