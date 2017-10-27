#!/bin/bash
for i in {1..50}
do
    curl -X POST -F "name=team-$i" -F 'password=test123' -F 'cpassword=test123' http://localhost:8080/register
done