#!/usr/bin/env bash

# possible environment variables:
# $TRACES_FOLDER : folder where traces files are generated
# $CHARTS_FOLDER : folder where chart reports is created
# $CHART_TEMPLATE : based on which chart is created

cd $(dirname $0)
cd ../

curl -H "Accept: application/zip" https://github.com/scalecube/trace-reporter/releases/download/v0.0.5/trace-reporter-0.0.5-SNAPSHOT-jar-with-dependencies.jar -o ./target/reporter-1.jar
ls ./target/
java \
    -jar ./target/reporter-1.jar -i 