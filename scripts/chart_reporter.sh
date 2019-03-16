#!/usr/bin/env bash

# possible environment variables:
# $TRACES_FOLDER : folder where traces files are generated
# $CHARTS_FOLDER : folder where chart reports is created
# $CHART_TEMPLATE : based on which chart is created

cd $(dirname $0)
cd ../

wget https://github.com/scalecube/trace-reporter/blob/latest/release/trace-reporter.jar?raw=true -O ./target/reporter-1.jar

java \
    -jar ./target/reporter-1.jar -i ./target/traces/ -o ./target/charts -t ./src/main/resources/chart_template.json