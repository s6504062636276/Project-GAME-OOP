#!/bin/sh
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/*.java && java -cp out Main
