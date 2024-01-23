# Deploying with Helm
You can run a full setup, using the Helm. 

## Preparations 
Make sure you have created the containers and stored them in a registry. See [README.md](../../../README.md#containers).

After building the containers copy the `values.yaml` and update the values to match your setup.

## Deploying
To deploy the helm chart run the following from the root of the project, assuming you copied the `values.yaml` as
`your-values.yaml` to the root of the project.

```shell
$ helm install moj ./src/deploy/helm --namespace moj --values your-values.yaml 
```

The worker needs to connect to the controller and the controller needs to connect keycloak to start property. This will
give container restarts during startup, but eventually it will all sort itself out and end up in a running state.

### Adding users
Once the environment is up, you need to log into the keycloak console located
at http(s)://${global.iam.ingress.host. You can use your specified username and password. Create a new user in the `moj` 
realm and add it to the `admin` group.

## GUIs
You can find the game at http(s)://${global.controller.ingress.host} and the game master console at 
http(s)://${global.controller.ingress.host/control
