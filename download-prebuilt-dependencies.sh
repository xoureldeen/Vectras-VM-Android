#!/bin/bash

DIR=$(dirname $0)
pushd $DIR

DEPVER=15

if which wget
then
  wget -c https://github.com/iiordanov/remote-desktop-clients/releases/download/dependencies/remote-desktop-clients-libs-${DEPVER}.tar.gz
else
  curl -L https://github.com/iiordanov/remote-desktop-clients/releases/download/dependencies/remote-desktop-clients-libs-${DEPVER}.tar.gz -o remote-desktop-clients-libs-${DEPVER}.tar.gz
fi

rm -rf remoteClientLib/jni/libs/deps/FreeRDP/
mkdir -p remoteClientLib/jni/libs/deps/FreeRDP/

tar xf remote-desktop-clients-libs-${DEPVER}.tar.gz

if [ ! -d remoteClientLib/src/main/jniLibs -a -d remoteClientLib/libs/ ]
then
  rm -f remoteClientLib/src/main/jniLibs
  cp -a remoteClientLib/libs/ remoteClientLib/src/main/jniLibs/
fi

echo "Done downloading and extracting dependencies."
popd
