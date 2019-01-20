#!/bin/bash
set -e

IMAGE=trezor-android

docker build -t "$IMAGE" .
docker run -it -v $(pwd):/src:z --user="$(stat -c "%u:%g" .)" "$IMAGE" ./gradlew assembleRelease
