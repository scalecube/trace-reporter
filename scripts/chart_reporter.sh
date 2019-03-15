#!/usr/bin/env bash

# possible environment variables:
# $TRACES_FOLDER : folder where traces files are generated
# $CHARTS_FOLDER : folder where chart reports is created
# $CHART_TEMPLATE : based on which chart is created

cd $(dirname $0)
cd ../

JAR_FILE=$(find . -type f -name "*jar-with*.jar")

java \
    -jar ${JAR_FILE}