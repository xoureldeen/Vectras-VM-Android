#!/bin/bash -e

SKIP_BUILD=false

function usage () {
  echo "$0 bVNC|freebVNC|aSPICE|freeaSPICE|aRDP|freeaRDP|CustomVncAnyPackageName|\
CustomRdpAnyPackageName|CustomSpiceAnyPackageName|libs|remoteClientLib|Opaque /path/to/your/android/sdk"
  exit 1
}

function clean_libs () {
  filter=$1
  dir=$2
  for f in $(find $dir -type f -name \*.so)
  do
    if ! echo $f | egrep -q "$filter"
    then
      rm -f $f
    fi
  done
}

function install_ndk() {
    DIR=$1
    VER=$2

    pushd ${DIR} >&/dev/null
    if [ ! -e android-ndk-${VER} ]
    then
        wget https://dl.google.com/android/repository/android-ndk-${VER}-linux-x86_64.zip  >&/dev/null
        unzip android-ndk-${VER}-linux-x86_64.zip >&/dev/null
    fi
    popd >&/dev/null
    echo $(realpath ${DIR}/android-ndk-${VER})
}


if [ "$1" == "--skip-build" ]
then
  SKIP_BUILD=true
  shift
fi

DIR=$(dirname $0)

. ${DIR}/../remoteClientLib/jni/libs/build-deps.conf

pushd $DIR

PRJ="$1"
export ANDROID_SDK="$2"

if [[ "$PRJ" != "bVNC" && "$PRJ" != "freebVNC" \
  && "$PRJ" != "aSPICE" && "$PRJ" != "freeaSPICE" \
  && "$PRJ" != "aRDP" && "$PRJ" != "freeaRDP" \
  && "$PRJ" =~ "^Custom.*" \
  && "$PRJ" != "libs" && "$PRJ" != "remoteClientLib" \
  && "$PRJ" != "Opaque" || "$ANDROID_SDK" == "" ]]
then
  usage
fi

if echo ${PRJ} | grep -qi "VNC"
then
  CLIENT_TYPE_VNC=true
  CUSTOM_MANIFEST_EXTENSION=CustomVnc
elif echo ${PRJ} | grep -qi "RDP"
then
  CLIENT_TYPE_RDP=true
  CUSTOM_MANIFEST_EXTENSION=CustomRdp
elif echo ${PRJ} | grep -qi "SPICE"
then
  CLIENT_TYPE_SPICE=true
  CUSTOM_MANIFEST_EXTENSION=CustomSpice
fi

if [ "$PRJ" == "libs" ]
then
  BUILDING_DEPENDENCIES=true
elif echo ${PRJ} | grep -q Custom
then
  CUSTOM_CLIENT=true
  ORIG_PRJ=${PRJ}
  PRJ=$(echo ${PRJ} | sed 's/^Custom//')
fi

if [ -n "${CUSTOM_CLIENT}" ]
then
  if ! grep -q CUSTOM_.*_APP_PACKAGE_NAME ../${CUSTOM_MANIFEST_EXTENSION}-app/src/main/AndroidManifest.xml
  then
    echo "Failed to find CUSTOM_.*_APP_PACKAGE_NAME in manifest."
    exit 1
  fi

  sed -i.bakautocustom "s/CUSTOM_.*_APP_PACKAGE_NAME/$PRJ/" ../${CUSTOM_MANIFEST_EXTENSION}-app/src/main/AndroidManifest.xml

  if grep -q CUSTOM_.*_APP_PACKAGE_NAME ../${CUSTOM_MANIFEST_EXTENSION}-app/src/main/AndroidManifest.xml
  then
    echo "Failed to set CUSTOM_.*_APP_PACKAGE_NAME in manifest."
    exit 1
  fi
fi

if [ "$SKIP_BUILD" == "false" ]
then
  pushd ../remoteClientLib/jni/libs
  echo "Automatically installing Android NDK"
  export ANDROID_NDK=$(install_ndk ./ ${ndk_version})
  ./build-deps.sh -j 8 build $PRJ
  popd

  if echo $PRJ | grep -qi "SPICE\|Opaque\|libs\|remoteClientLib"
  then
    pushd ../remoteClientLib
    export NDK_LIBS_OUT=./src/main/jniLibs
    ${ANDROID_NDK}/ndk-build -j 2

    echo "Adding any custom certificate authority files in $(pwd)/certificate_authorities/ to certificate bundle from gstreamer."
    if [ -n "$(ls certificate_authorities/)" ]
    then
      for ca in certificate_authorities/*
      do
        echo Adding ${ca} to gstreamer provided ca-certificates.crt
        cat ${ca} >> src/main/assets/ssl/certs/ca-certificates.crt
      done
    fi
    popd
  fi
fi

if [ -n "$BUILDING_DEPENDENCIES" ]
then
  echo "Done building libraries"
fi

freerdp_libs_dir=../remoteClientLib/jni/libs/deps/FreeRDP/client/Android/Studio/freeRDPCore/src/main/jniLibs
freerdp_libs_link=../remoteClientLib/jni/libs/deps/FreeRDP/client/Android/Studio/freeRDPCore/src/main/libs
if echo $PRJ | grep -iq "VNC"
then
  clean_libs "sqlcipher" libs/
  clean_libs "sqlcipher" ../remoteClientLib/libs/
  clean_libs "sqlcipher" ../remoteClientLib/src/main/jniLibs/
  [ -d ${freerdp_libs_dir} ] && rm -rf ${freerdp_libs_dir}.DISABLED && mv ${freerdp_libs_dir} ${freerdp_libs_dir}.DISABLED
  rm -rf ${freerdp_libs_link}
elif echo $PRJ | grep -iq "RDP"
then
  clean_libs "sqlcipher" libs/
  clean_libs "sqlcipher" ../remoteClientLib/libs/
  clean_libs "sqlcipher" ../remoteClientLib/src/main/jniLibs/
  [ -d ${freerdp_libs_dir}.DISABLED ] && rm -rf ${freerdp_libs_dir} && mv ${freerdp_libs_dir}.DISABLED ${freerdp_libs_dir}
  rm -rf ${freerdp_libs_link}
  ln -s jniLibs ${freerdp_libs_link}
elif echo $PRJ | grep -iq "SPICE\|Opaque"
then
  [ -d ${freerdp_libs_dir} ] && rm -rf ${freerdp_libs_dir}.DISABLED && mv ${freerdp_libs_dir} ${freerdp_libs_dir}.DISABLED
  rm -rf ${freerdp_libs_link}
elif echo $PRJ | grep -iq "libs"
then
  [ -d ${freerdp_libs_dir}.DISABLED ] && rm -rf ${freerdp_libs_dir} && mv ${freerdp_libs_dir}.DISABLED ${freerdp_libs_dir}
  rm -rf ${freerdp_libs_link}
  ln -s jniLibs ${freerdp_libs_link}
fi

popd
echo
echo "Now please switch to your IDE, select a launch configuration for the project you want to run"
echo "and then clean and rebuild the project before running."
