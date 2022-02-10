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

a=$(docker-compose --help)

echo "compose: $a"

#docker-compose build --no-cache --build-arg ENVPROFILE=$target
docker-compose build --build-arg ENVPROFILE=$target
docker-compose up --force-recreate --no-deps -d api