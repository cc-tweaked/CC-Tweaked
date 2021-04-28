#!/usr/bin/env sh
set -e

test -d bin || mkdir bin
test -f bin/illuaminate || curl -s -obin/illuaminate https://squiddev.cc/illuaminate/linux-x86-64/illuaminate
chmod +x bin/illuaminate

./gradlew luaJavadoc
bin/illuaminate lint
