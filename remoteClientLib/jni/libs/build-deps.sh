#!/bin/bash
#
# A script for building VMNetX dependencies for Android
# Based on build.sh from openslide-winbuild
#
# Copyright (c) 2017 Iordan Iordanov
# Copyright (c) 2011-2015 Carnegie Mellon University
# All rights reserved.
#
# This script is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the Free
# Software Foundation; either version 2 of the License, or (at your option)
# any later version.
#
# This script is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
# more details.
#
# You should have received a copy of the GNU General Public License along
# with this script.  If not, see <http://www.gnu.org/licenses/>.
#

set -eE

. build-deps.conf
. build-deps-functions.sh

expand() {
    # Print the contents of the named variable
    # $1  = the name of the variable to expand
    echo "${!1}"
}

tarpath() {
    # Print the tarball path for the specified package
    # $1  = the name of the program
    case "$1" in
    configguess)
        echo "tar/config.guess"
        ;;
    configsub)
        echo "tar/config.sub"
        ;;
    *)
        echo "tar/$(basename $(expand ${1}_url))"
        ;;
    esac
}

fetch() {
    # Fetch the specified package
    # $1  = package shortname
    local url
    url="$(expand ${1}_url)"
    mkdir -p tar
    if [ ! -e "$(tarpath $1)" ] ; then
        echo "Fetching ${1}..."
        case "$1" in
        configguess|configsub)
            wget -q -O "$(tarpath $1)" "$url"
            ;;
        *)
            echo wget -P tar -q "$url"
            wget -P tar -q "$url"
            ;;
        esac
    fi
}

unpack() {
    # Remove the package build directory and re-unpack it
    # $1  = package shortname
    local path
    fetch "${1}"
    mkdir -p "${build}"
    path="${build}/$(expand ${1}_build)"
    echo "Unpacking ${1}..."
    rm -rf "${path}"
    tar xf "$(tarpath $1)" -C "${build}"
}

is_built() {
    # Return true if the specified package is already built
    # $1  = package shortname
    local artifact
    for artifact in $(expand ${1}_artifacts)
    do
        if [ ! -e "${root}/lib/${artifact}" ] ; then
            return 1
        fi
    done
    return 0
}

do_configure() {
    if [ "$1" == "install_in_gst" ]
    then
        shift
        prefix="${gst}"
    else
        prefix="${root}"
    fi
    # Run configure with the appropriate parameters.
    # Additional parameters can be specified as arguments.
    #
    # Fedora's ${build_host}-pkg-config clobbers search paths; avoid it
    #
    # Use only our pkg-config library directory, even on cross builds
    # https://bugzilla.redhat.com/show_bug.cgi?id=688171
    echo Current working directory: $(pwd)
    echo Executing: ./configure \
            --host=${build_host} \
            --build=${build_system} \
            --prefix=\"${prefix}\" \
            --enable-static \
            --disable-shared \
            --disable-tests \
            --disable-examples \
            --disable-dependency-tracking \
            PKG_CONFIG=\"pkg-config --static\" \
            PKG_CONFIG_LIBDIR=\"${root}/share/pkgconfig:${root}/lib/pkgconfig:${gst}/lib/pkgconfig\" \
            PKG_CONFIG_PATH= \
            CPPFLAGS=\"${cppflags} -I${root}/include -I${gst}/include -I${root}/include/libusb-1.0\" \
            CFLAGS=\"${cflags}\" \
            CXXFLAGS=\"${cxxflags}\" \
            LDFLAGS=\"${ldflags} -L${root}/lib -L${gst}/lib\" \
            "$@"

    echo Environment:
    env

    ./configure \
            --host=${build_host} \
            --build=${build_system} \
            --prefix="${prefix}" \
            --enable-static \
            --disable-shared \
            --disable-dependency-tracking \
            PKG_CONFIG="pkg-config --static" \
            PKG_CONFIG_LIBDIR="${root}/share/pkgconfig:${root}/lib/pkgconfig:${gst}/lib/pkgconfig" \
            PKG_CONFIG_PATH= \
            CPPFLAGS="${cppflags} -I${root}/include -I${gst}/include -I${root}/include/libusb-1.0" \
            CFLAGS="${cflags}" \
            CXXFLAGS="${cxxflags}" \
            LDFLAGS="${ldflags} -L${root}/lib -L${gst}/lib" \
            "$@"
}

build_one() {
    # Build the specified package if not already built
    # $1  = package shortname
    local basedir builddir

    if is_built "$1" ; then
        echo "Dependency ${1} already built, skipping."
        return
    fi

    unpack "$1"

    echo "Building ${1}..."
    basedir="$(pwd)"
    builddir="${build}/$(expand ${1}_build)"
    pushd "$builddir" >/dev/null
    case "$1" in
    celt)
        cp "${basedir}/$(tarpath configsub)" .
        do_configure \
                --without-ogg
        cd libcelt
        make $parallel
        make install
        cd ..
        mkdir -p "${root}/lib/pkgconfig"
        cp -a celt051.pc "${root}/lib/pkgconfig"
        ;;
    openssl)
        local os
        case "$abi" in
        armeabi)
            os=android
            arch=arm
            ;;
        armeabi-v7a)
            os=android-arm
            arch=arm
            ;;
        arm64-v8a)
            os=android-arm64
            arch=arm64
            ;;
        x86)
            os=android-x86
            arch=x86
            ;;
        x86_64)
            os=android-x86_64
            arch=x86_64
            ;;
        *)
            echo "Unsupported ABI: $abi"
            exit 1
            ;;
        esac

        export PATH=${ndkdir}/toolchains/llvm/prebuilt/linux-x86_64/bin/:${PATH}

        echo Environment:
        env

        echo Executing:
        echo ./Configure \
                "${os}" \
                --prefix="$root" \
                no-zlib \
                no-hw \
                no-ssl2 \
                no-ssl3 \
                -D__ANDROID_API__=${android_api} \
                ${cppflags} \
                ${cflags} \
                ${ldflags}

        echo Current working directory: $(pwd)
        ./Configure \
                "${os}" \
                --prefix="$root" \
                no-zlib \
                no-hw \
                no-ssl2 \
                no-ssl3 \
                -D__ANDROID_API__=${android_api} \
                ${cppflags} \
                ${cflags} \
                ${ldflags}
        make depend
        make
        make install_sw
        ;;
    spiceprotocol)
        autoreconf -fi
        do_configure
        make $parallel
        make install
        ;;
    spicegtk)
        autoreconf -fi
        do_configure \
                --with-gtk=no \
                --enable-dbus=no \
                --enable-controller=no \
                --enable-gstaudio \
                --disable-celt051 \
                --enable-opus \
                LIBS="-lm"

	# Disable tests and tools
        sed -i 's/tests//' subprojects/spice-common/Makefile || true
        sed -i 's/tests//' spice-common/Makefile || true
        sed -i 's/tests//' Makefile
        sed -i 's/tools//' Makefile

        patch -p0 < "${basedir}/spice-gtk-log.patch"
        patch -p1 < "${basedir}/spice-gtk-exit.patch"
        patch -p1 < "${basedir}/spice-gtk-disable-agent-sync-audio-calls.patch"
        patch -p0 < "${basedir}/spice-gtk-disable-mm-time-reset.patch"
        make $parallel

        # Patch to avoid SIGBUS due to unaligned accesses on ARM7
        patch -p1 < "${basedir}/spice-marshaller-sigbus.patch"
        make $parallel

        make install

        # Put some header files in a version-independent location.
        for f in config.h _builddir/subprojects/spice-common/common _builddir/config.h tools/*.h src/*.h spice-common/common subprojects/spice-common/common
        do
            rsync -a $f ${root}/include/spice-1/ || true
        done

        ;;
    soup)
        #gtkdocize
        echo "EXTRA_DIST =" > gtk-doc.make
        echo "CLEANFILES =" >> gtk-doc.make
        intltoolize --automake --copy
        autoreconf -fi
        do_configure \
                --enable-introspection=no \
                --without-gnome \
                --disable-tests \
                --disable-examples \
                --disable-glibtest
        make $parallel
        make install
        ;;
    rest)
        echo "EXTRA_DIST =" > gtk-doc.make
        echo "CLEANFILES =" >> gtk-doc.make
        autoreconf -fi
        do_configure \
                --enable-introspection=no \
                --without-gnome \
                --disable-tests \
                --disable-examples \
                --disable-rest-examples \
                --disable-gtk-doc
        make $parallel
        make install
        ;;
    govirt)
        ./autogen.sh || /bin/true
        #patch -p0 < "${basedir}/libgovirt-status.patch"
        patch -p0 < "${basedir}/libgovirt-tests.patch"
        do_configure \
                --enable-introspection=no \
                --without-gnome \
                --enable-tests=no
        make $parallel
        make install
        ;;
    usb)
        autoreconf -fi
        do_configure \
                --disable-udev
        make $parallel
        make install
        ;;
    usbredir)
        autoreconf -fi
        do_configure
        make $parallel
        make install
        ;;
    gmp)
        autoreconf -fi
        do_configure
        make $parallel
        make install
        ;;
    nettle)
        autoreconf -fi
        do_configure --enable-mini-gmp
        make $parallel
        make install
        ;;
    esac

    popd >/dev/null
}

sdist() {
    # Build source distribution
    local package tardir
    tardir="vmnetx-android-dependencies"
    rm -rf "${tardir}"
    mkdir -p "${tardir}"
    for package in $packages
    do
        fetch "$package"
        cp "$(tarpath ${package})" "${tardir}"
    done
    rm -f "${tardir}.tar.gz"
    tar czf "${tardir}.tar.gz" "${tardir}"
    rm -r "${tardir}"
}

setup() {
    # Configure the build environment and set up variables
    # $1 = ABI
    local system_arg
    if [ -z "${origpath}" ] ; then
        origpath="$PATH"
    fi

    cppflags=""
    cflags="-O2 -std=gnu99 -Dtypeof=__typeof__ -fPIC"
    cxxflags="${cflags}"
    ldflags=""

    abi="${1}"
    case "$abi" in
    armeabi)
        gstarch=arm
        arch=arm
        build_host="arm-linux-androideabi"
        ;;
    armeabi-v7a)
        gstarch=armv7
        arch=arm
        build_host="arm-linux-androideabi"
        cflags="${cflags} -march=armv7-a -mfloat-abi=softfp -mfpu=neon"
        ldflags="${ldflags} -march=armv7-a -Wl,--fix-cortex-a8"
        ;;
    arm64-v8a)
        gstarch=arm64
        arch=arm64
        build_host="aarch64-linux-android"
        ;;
    x86)
        gstarch=x86
        arch=x86
        build_host="i686-linux-android"
        ;;
    x86_64)
        gstarch=x86_64
        arch=x86_64
        build_host="x86_64-linux-android"
        ;;
    *)
        echo "Unknown ABI: $abi"
        exit 1
    esac

    build="deps/${abi}/build"
    root="$(pwd)/deps/${abi}/root"
    toolchain="$(pwd)/deps/${abi}/toolchain"
    gst="$(pwd)/deps/${abi}/gstreamer"
    mkdir -p "${root}"
    [ -e ${gst} ] && mkdir -p ${gst}/etc/ssl/certs/ && cp /etc/ssl/certs/ca-certificates.crt ${gst}/etc/ssl/certs/ || /bin/true

    fetch configguess
    build_system=$(sh tar/config.guess)

    # Set up build environment
    if ! [ -e "${toolchain}/bin/${build_host}-gcc" ] ; then
        if [ -z "${ndkdir}" ] ; then
            echo "No toolchain configured and NDK directory not set."
            exit 1
        fi
        echo "Toolchain for ${arch} not found, building it."
        ${ndkdir}/build/tools/make_standalone_toolchain.py \
                --api "${android_api}" \
                --arch "${arch}" \
                --install-dir "${toolchain}"
#                --deprecated-headers \
    fi
    if ! [ -e "${toolchain}/bin/${build_host}-gcc" ] ; then
        echo "Couldn't configure compiler."
        exit 1
    fi
    PATH="${toolchain}/bin:${origpath}"
    git clone ${setuptools_url} -b ${setuptools_ver} || true
    pushd setuptools
    python bootstrap.py
    python setup.py install
    popd
}

build() {
    # Build binaries
    # $1 = ABI
    local package pkgstr origroot

    # Set up build environment
    setup "$1"
    fetch configsub

    if [ -f CERBERO_BUILT_${1} ]
    then
        echo ; echo
        echo "Cerbero was previously built. Remove $(realpath CERBERO_BUILT_${1}) if you want to rebuild it."
        echo ; echo
    else
        git config --global protocol.file.allow always
        # Build GStreamer SDK
        if git clone https://gitlab.freedesktop.org/gstreamer/cerbero
        then
            pushd cerbero
            git checkout ${gstreamer_ver}
            patch -p1 < ../cerbero-disable-assrender.patch
            popd
            cerbero/cerbero-uninstalled bootstrap
            echo "allow_parallel_build = True" >>  cerbero/config/cross-android-universal.cbc
            echo "toolchain_prefix = \"${ndkdir}\"" >> cerbero/config/cross-android-universal.cbc
        fi

        echo "Copying local recipes into cerbero"
        git clone https://github.com/iiordanov/remote-desktop-clients-cerbero-recipes.git recipes || true
        pushd recipes
        git pull
        popd
        rsync -avP recipes/ cerbero/recipes/

        sed -i "s/version = '1.1.1.*'/version = '${openssl_ver}'/" cerbero/recipes/openssl.recipe
        sed -i "s/0b7a3e5e59c34827fe0c3a74b7ec8baef302b98fa80088d7f9153aa16fa76bd1/cf3098950cb4d853ad95c0841f1f9c6d3dc102dccfcacd521d93925208b76ac8/" cerbero/recipes/openssl.recipe

        echo "Running cerbero build for $1 in $(pwd)"
        cerbero/cerbero-uninstalled -c cerbero/config/cross-android-universal.cbc build \
          gstreamer-1.0 glib glib-networking libxml2 pixman libsoup openssl cairo json-glib gst-android-1.0 gst-plugins-bad-1.0 gst-plugins-good-1.0 gst-plugins-base-1.0 gst-plugins-ugly-1.0 gst-libav-1.0 spiceglue

        echo "Copying spice-gtk header files that it does not install automatically"
        SPICEDIR=$(ls -d1 cerbero/build/sources/android_universal/${gstarch}/spice-gtk-* | tail -n 1)

        # Workaround for non-existent lib-pthread.la dpendency snaking its way into some of the libraries.
        sed -i 's/[^ ]*lib-pthread.la//' cerbero/build/dist/android_universal/*/lib/*la

        # Prepare gstreamer for current architecture
        if [ ! -e "${gst}/lib/libglib-2.0.a" ] ; then
            echo "Linking ../../cerbero/build/dist/android_universal/${gstarch} to ${gst}"
            ln -sf "../../cerbero/build/dist/android_universal/${gstarch}" "${gst}"
            ls -ld "${gst}"
        fi


        for f in ${SPICEDIR}/config.h ${SPICEDIR}/_builddir/subprojects/spice-common/common ${SPICEDIR}/_builddir/config.h ${SPICEDIR}/tools/*.h ${SPICEDIR}/src/*.h ${SPICEDIR}/spice-common/common ${SPICEDIR}/subprojects/spice-common/common
        do
            rsync -a $f ${gst}/include/spice-1/ || true
        done
        touch CERBERO_BUILT_${1}
    fi

    # Build
    for package in $packages
    do
        build_one "$package"
    done
}

clean() {
    # Clean built files
    local package artifact curabi
    if [ $# -gt 0 ] ; then
        for package in "$@"
        do
            echo "Cleaning ${package}..."
            for curabi in $abis; do
                setup "${curabi}"
                for artifact in $(expand ${package}_artifacts)
                do
                    rm -f "${root}/lib/${artifact}"
                done
            done
        done
    else
        echo "Cleaning..."
        rm -rf deps
    fi
}

updates() {
    # Report new releases of software packages
    local package url curver newver
    for package in $packages gstreamer
    do
        url="$(expand ${package}_upurl)"
        if [ -z "$url" ] ; then
            continue
        fi
        curver="$(expand ${package}_ver)"
        newver=$(wget -q -O- "$url" | \
                sed -nr "s%.*$(expand ${package}_upregex).*%\\1%p" | \
                sort -uV | \
                tail -n 1)

        if [ "${curver}" != "${newver}" ] ; then
            printf "%-15s %10s  => %10s\n" "${package}" "${curver}" "${newver}"
        fi
    done
}

fail_handler() {
    # Report failed command
    echo "Failed: $BASH_COMMAND (line $BASH_LINENO)"
    exit 1
}

build_freerdp() {

    if [ -f FREERDP_BUILT ]
    then
      echo ; echo
      echo "FreeRDP was previously built. Remove $(realpath FREERDP_BUILT) if you want to rebuild it."
      echo ; echo
      return
    fi

    pushd deps
    basedir="$(pwd)"

    missing_artifact="false"
    for abi in $abis
    do
      for f in ${freerdp_artifacts}
      do
        if [ ! -f ${freerdp_build}/client/Android/Studio/freeRDPCore/src/main/jniLibs/${abi}/${f} -a \
             ! -f ${freerdp_build}/client/Android/Studio/freeRDPCore/src/main/jniLibs.DISABLED/${abi}/${f} ]
        then
          missing_artifact="true"
        fi
      done
    done

    if [ $missing_artifact == "true" ]
    then
        if [ ! -d ${freerdp_build}/.git/ ]
        then
            rm -rf ${freerdp_build}/
            git clone ${freerdp_url}
        fi

        pushd ${freerdp_build}
        git fetch
        git checkout ${freerdp_ver}
        git reset --hard

        # Patch the config
        sed -i -e 's/CMAKE_BUILD_TYPE=.*/CMAKE_BUILD_TYPE=Release/'\
               -e 's/WITH_JPEG=.*/WITH_JPEG=1/'\
               -e 's/WITH_OPENH264=.*/WITH_OPENH264=1/'\
               -e 's/OPENH264_TAG=.*/OPENH264_TAG=v2.1.1/'\
               -e 's/OPENSSL_TAG=.*/OPENSSL_TAG=openssl-1.1.1w/'\
               -e 's/NDK_TARGET=26/NDK_TARGET=21/'\
               -e 's/453afa66dacb560bc5fd0468aabee90c483741571bca820a39a1c07f0362dc32/af173e90fce65f80722fa894e1af0d6b07572292e76de7b65273df4c0a8be678/'\
               -e 's/d7939ce614029cdff0b6c20f0e2e5703158a489a72b2507b8bd51bf8c8fd10ca/cf3098950cb4d853ad95c0841f1f9c6d3dc102dccfcacd521d93925208b76ac8/'\
               -e "s/BUILD_ARCH=.*/BUILD_ARCH=\"${abis}\"/"\
                ./scripts/android-build.conf

        for f in ${basedir}/../*_freerdp_*.patch
        do
            echo "Applying $f"
            patch -N -p1 < ${f}
        done

        sed -i 's/implementationSdkVersion/compileSdkVersion/; s/.*rootProject.ext.versionName.*//; s/.*rootProject.ext.versionCode.*//; s/.*.*buildToolsVersion.*.*//; s/compile /implementation /; s/minSdkVersion .*/minSdkVersion 14/;' \
               client/Android/Studio/freeRDPCore/build.gradle

        cp "${basedir}/../freerdp_AndroidManifest.xml" client/Android/Studio/freeRDPCore/src/main/AndroidManifest.xml

        echo "Installing android NDK ${freerdp_ndk_version} for FreeRDP build compatibility"
        export ANDROID_NDK=$(install_ndk ../../ ${freerdp_ndk_version})
        export OPENH264_NDK=$(install_ndk ../../ ${freerdp_openh264_ndk_version})
        echo "Android NDK version for FreeRDP ${ndk_version} is installed at ${ANDROID_NDK}"
        echo "Installing cmake ${freerdp_cmake_version} for FreeRDP build compatibility"
        export CMAKE_PATH=$(install_cmake ../../ ${freerdp_cmake_version})/bin
        export PATH=${CMAKE_PATH}:${PATH}
        export CMAKE_PROGRAM=${CMAKE_PATH}/cmake
        ./scripts/android-build-freerdp.sh

        # Prepare the FreeRDPCore project for use as a library
        rm -f ../../../../../freeRDPCore
        ln -s remoteClientLib/jni/libs/deps/${freerdp_build}/client/Android/Studio/freeRDPCore/ ../../../../../freeRDPCore
        popd
    fi

    popd
    touch FREERDP_BUILT
}


# Set up error handling
trap fail_handler ERR

# Parse command-line options
parallel=""
export ANDROID_NDK=$(install_ndk ./ ${ndk_version})
echo "Android NDK version ${ndk_version} is installed at ${ANDROID_NDK}"
ndkdir="${ANDROID_NDK}"
origdir="$(pwd)"

while getopts "j:n:" opt
do
    case "$opt" in
    j)
        parallel="-j${OPTARG}"
        ;;
    n)
        ndkdir="${OPTARG}"
        ;;
    esac
done
shift $(( $OPTIND - 1 ))

# Process command-line arguments
case "$1" in
sdist)
    sdist
    ;;
build)
    for curabi in $abis
    do
        build "$curabi"
    done

    build_freerdp

    echo
    echo "Now you can run ndk-build if building aSPICE."
    echo
    echo "Run the following command:"
    echo "cd ../../ ; ndk-build"
    echo

    ;;
clean)
    shift
    clean "$@"
    ;;
updates)
    updates
    ;;
*)
    cat <<EOF
Usage: $0 sdist
       $0 [-j<parallelism>] [-n<ndk-dir>] build
       $0 clean [package...]
       $0 updates

Packages:
$packages
EOF
    exit 1
    ;;
esac

exit 0
