#!/usr/bin/env bash

. ./scripts/target/build-env.sh

echo -n "Waiting for npm dev server to start."

until [ -e $NPM_DEV_PATH ]; do
    echo -n "."
    sleep 2
done

echo "  ✅"
echo "NPM dev server started."
echo "Waiting for client-fastopt/main.js to be generated."

until [ -e $MAIN_JS_PATH ]; do
    echo -n "."
    sleep 2
done
echo "  ✅"
echo "⏱️ Watching client-fastopt/main.js for changes..."


DEV=1 sbt '~client/fastLinkJS'
