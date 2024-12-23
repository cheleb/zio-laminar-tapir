#!/usr/bin/env bash

. ./scripts/env.sh

rm -f $NPM_DEV_STARTED

if [ ! -e $BUILD_ENV_FILE ]; then
    echo "Waiting for $BUILD_ENV_FILE to be generated..."
    echo '  Import the project !!!'
    echo

    until [ -e $BUILD_ENV_FILE ]; do
        echo -n "."
        sleep 4
    done

    echo
    echo
    echo " Good job 🚀"
    echo

fi

filename_lock=node_modules/.package-lock.json

cd examples/client

if [ ! -f "$filename_lock" ]; then
    echo "First time setup: Installing npm dependencies..."

    npm i
else
    npmRefresh=3600
    filename=package.json
    age=$(($(date +%s) - $(stat -t %s -f %m -- "$filename")))
    age_lock=$(($(date +%s) - $(stat -t %s -f %m -- "$filename_lock")))
    if [ $age_lock -gt $npmRefresh ] || [ $age_lock -gt $age ]; then
        echo "Reinstalling npm dependencies..."
        if [ $age_lock -gt $age ]; then
            echo "(New dependencies)"
        else
            echo "(Refreshing dependencies: package-lock.json is older than $npmRefresh seconds ($age_lock).)"
        fi

        npm i
        cd ../..
    else
        echo "Skipping npm install... $filename_lock is less than an $npmRefresh seconds old ($age)."
    fi
fi
