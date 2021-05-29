#!/usr/bin/env sh
set -e

test -d bin || mkdir bin
test -f bin/illuaminate || curl -s -obin/illuaminate https://squiddev.cc/illuaminate/linux-x86-64/illuaminate
chmod +x bin/illuaminate

if [ -n ${GITHUB_ACTIONS+x} ]; then
    # Register a problem matcher (see https://github.com/actions/toolkit/blob/master/docs/problem-matchers.md)
    # for illuaminate.
    echo "::add-matcher::.github/matchers/illuaminate.json"
    trap 'echo "::remove-matcher owner=illuaminate::"' EXIT
fi

./gradlew luaJavadoc
bin/illuaminate lint
