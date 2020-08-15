#!/bin/bash
#docker image build --squash -t registry.cloud.bliep.net/moj/base-jdk14:latest -f Dockerfile-jdk14 .
docker image build --squash -t registry.cloud.bliep.net/moj/base-jdk11:latest -f Dockerfile-jdk11 .