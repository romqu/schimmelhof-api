#!/bin/bash
set -o errexit -o pipefail -o noclobber -o nounset

while getopts t: flag; do
  case "${flag}" in
  t) target=${OPTARG} ;;
  *)
    echo "Unknown parameter passed: $1"
    exit 1
    ;;
  esac
done

echo "Deploying target: $target"

./gradlew assemble "-Dorg.gradle.jvmargs=--illegal-access=permit -Dspring.profiles.active=$target"

sudo docker-compose build --no-cache --build-arg ENVPROFILE=$target --build-arg JAR_FILE=build/libs/\*.jar api
sudo docker-compose up --force-recreate --no-deps -d api