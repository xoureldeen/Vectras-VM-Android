#!/bin/bash -e

DIR=$(dirname $0)
pushd $DIR

DEPVER=$1

if [ -z "$DEPVER" ]
then
  echo "$0 dependencies_version"
  echo "Example: $0 1"
  exit 1
fi

PROJECT=libs
./bVNC/prepare_project.sh $PROJECT $ANDROID_NDK $ANDROID_SDK

tar --exclude='remoteClientLib/jni/libs/deps/FreeRDP/client/Android/Studio/freeRDPCore/build/intermediates/*' \
    -c -z -f remote-desktop-clients-libs-${DEPVER}.tar.gz \
    remoteClientLib/src/main/jniLibs \
    remoteClientLib/libs \
    bVNC/libs \
    remoteClientLib/jni/libs/deps/FreeRDP/client/Android/Studio/freeRDPCore/

echo "Done creating new dependencies archive."
echo "Differences in repo at present:"
git diff
popd
