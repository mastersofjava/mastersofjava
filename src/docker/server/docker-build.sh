#!/bin/bash
cp ../../../target/server-0.0.1-SNAPSHOT.jar .
docker build -t registry.cloud.bliep.net/moj/moj-game-server:latest .
rm server-0.0.1-SNAPSHOT.jar
