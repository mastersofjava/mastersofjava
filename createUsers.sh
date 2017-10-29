#!/bin/bash
for i in {1..50}
do
    curl -X POST -F "name=team$i" -F "password=team$i" -F "cpassword=team$i" -F "country=Nederland" -F "company=First8" http://localhost:8080/register
done