# Deploying with Docker Compose
For deploying with Docker Compose there are two options.

* Deploy and All-In-One solution.
* Deploy all components one by one.

## Prerequisite

* Build the images as described [here](../../../README.md#building-containers).
* Update hosts file to contain the line below so IAM will work.
  ```
  127.0.0.1 host.docker.internal
  ```

## All-In-One Deployment
For the all All-In-One deployment a [docker-compose.yaml](all-in-one/docker-compose.yaml) example file can be found in 
the [all-in-one](all-in-one) directory.

### Starting
Once the docker-compose.yaml files are updated to match your environment the following can be used

```shell
$ docker compose up
```

*NOTE*: The worker needs to connect to the controller and the controller needs to connect keycloak to start property. This will
give errors during startup, but eventually it will all sort itself out and end up in a running state. Docker compose
depends_on sadly does not wait for containers to be started.

### Adding Users
Once the environment is up, you need to log into the [keycloak console](http://host.docker.internal:8888) with
username `keycloak` and password `keycloak`. Create a new user in the `moj` realm and add it to the `admin` group.

### GUIs
You can find the game [here](http://localhost:8080) and the game master console [here](http://localhost:8080/control).

## Component Deployment 
For the component deployment the following docker-compose example files are available:

* [postgresql](postgresql/docker-compose.yaml) in the folder [postgresql](postgresql)
* [iam](iam/docker-compose.yaml) in the folder [iam](iam)
* [controller](controller/docker-compose.yaml) in the folder [controller](controller)
* [worker](worker/docker-compose.yaml) in the folder [worker](worker)
* [single](single/docker-compose.yaml) in the folder [single](single)

### Starting
Once the docker-compose.yaml files for all needed components are updated to match your environment the following can be 
used for every component.

```shell
$ docker compose up
```
The order in which components should be started is as follows. Always make sure component is running correctly before
starting the next.

#### Controller Worker Setup

* postgresql
* iam
* controller
* worker

#### Single Setup

* postgresql
* iam
* single

