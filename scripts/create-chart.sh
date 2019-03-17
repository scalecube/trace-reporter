#!/usr/bin/env bash

# possible environment variables:
# $TRACES_FOLDER : folder where traces files are generated
# $CHARTS_FOLDER : folder where chart reports is created
# $CHART_TEMPLATE : based on which chart is created

LATEST_VERSION_JAR=https://github.com/scalecube/trace-reporter/blob/latest/release/trace-reporter.jar?raw=true

cd $(dirname $0)
cd ../

wget ${LATEST_VERSION_JAR} -O ./target/trace-reporter.jar

java \
    -jar ./target/trace-reporter.jar -i ./target/traces/ -o ./target/charts -t ./src/main/resources/chart_template.json