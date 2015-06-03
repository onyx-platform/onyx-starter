#!/bin/bash

set -e

lein clean

lein uberjar

docker build -t onyx-starter:0.1.0 .
