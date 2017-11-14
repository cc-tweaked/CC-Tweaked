#!/bin/sh

echo "Building with gradle..."
rm -rf build/libs
rm -rf build/resources
rm -rf build/classes
chmod -R +rw src/main/resources
chmod +x gradlew
./gradlew build

echo "Deleting old deployment..."
rm -rf deploy
mkdir deploy

echo "Making new deployment..."
INPUTJAR=`ls -1 build/libs | grep -v sources`
OUTPUTJAR=`ls -1 build/libs | grep -v sources | sed s/\-//g`
FRIENDLYNAME=`ls -1 build/libs | grep -v sources | sed s/\-/\ /g | sed s/\.jar//g`
cp build/libs/$INPUTJAR deploy/$OUTPUTJAR

echo "Done."
