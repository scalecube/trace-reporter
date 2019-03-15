#!/usr/bin/env bash

cd $(dirname $0)
cd ../

JAR_FILE=$(find . -type f -name "*jar-with*")

java \
    -jar ${JAR_FILE}