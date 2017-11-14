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

echo "Creating API..."
mkdir -p deploy/api/src/dan200/computercraft
cp -r build/sources/main/java/dan200/computercraft/api deploy/api/src/dan200/computercraft/api

echo "Creating API Javadocs..."
mkdir -p deploy/api/doc
cd src/main/java/dan200/computercraft/api
find . -type f -name "*.java" | xargs javadoc -d ../../../../../../deploy/api/doc -windowtitle "$FRIENDLYNAME"
cd ../../../../../..

echo "Adding API and Javadocs to deployment..."
cd deploy
zip -r $OUTPUTJAR api/doc > /dev/null
zip -r $OUTPUTJAR api/src/dan200/computercraft > /dev/null
cd ..
rm -rf deploy/api

echo "Done."
