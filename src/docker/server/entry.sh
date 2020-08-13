#!/bin/bash

JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom ${JAVA_OPTS}"
exec dockerize ${DOCKERIZE_OPTS} java ${JAVA_OPTS} $@
