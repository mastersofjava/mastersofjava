## Information
This test uses the 'Requirement Hell' assignment from the 2022 MoJ, one of - if not the - heaviest questions 
performancewise since it has many unittests that call eachother.


## Preparation
1. Start keycloak and the game server (see readme.md on root level).
2. Set the values of the 'Conf' class to the correct values. (follow instructions below for client secret)
3. Create the 2022-assignments.zip from https://bitbucket.org/first8/2022-assignments
4. Upload the 2022-assignments.zip in the /control
5. Start the requirement hell assignment in the /control

### Get Keycloak client secret
1. Go to keycloack and open realm 'moj'
2. Go to Clients
3. Add a client with ID and name 'gatling' (type OpenID Connect)
4. In the next page, enable 'Client authentication' and save
5. In the credentials tab, copy the client secret
6. Paste it in the final field in Conf.keyCloakClientSecret


## Run the script
run ```mvn gatling:test``` in the root directory of this project.

## Debugging
If you want more logging, you can use the logback.xml file in this folder by running with this command:
```mvn gatling:test -"Dlogback.configurationFile"=src\test\java\performance\logback.xml```