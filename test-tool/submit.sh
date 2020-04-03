#!/bin/zsh

rm -rf submit/src
cp -r src submit/src
typeset githash=$(git rev-parse HEAD)
cd submit
git add .
git commit -m "Sync commit $githash"
