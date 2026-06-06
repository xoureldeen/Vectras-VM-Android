
#Example for arm64-v8a
apk update
apk add --no-cache bash curl tar rsync flex build-base git meson bison ninja python3 py3-pip libcap-ng-dev glib-dev pixman-dev sdl2-dev sdl2_image-dev sndio alsa-utils alsaconf zlib-dev libaio-dev liburing-dev libcap libcap-ng libssh lzo snappy capstone libcbor libdw gtk+3.0-dev libssh-dev libnfs-dev libseccomp-dev lzo-dev snappy-dev capstone-dev ndctl-libs libcbor-dev libselinux libselinux-dev fuse-dev vde2-dev nmap sndio-dev pipewire-dev alsa-lib-dev vte3-dev keyutils keyutils-dev rng-tools nettle-dev libgcrypt libgcrypt-dev gnutls-dev iasl gcc-objc rust libudev-zero-dev ndctl-dev libu2f-server libu2f-server-dev libbpf libbpf-dev rdma-core-openrc curl-dev linux-pam linux-pam-dev net-snmp-dev jack-dev fuse3 fuse3-dev linux-virt
pip install Ninja Sphinx --break-system-packages
pip install meson --upgrade --break-system-packages
pip install sphinx-rtd-theme --break-system-packages
export CFLAGS="-O2 -march=armv8-a+crc+nosve"
export CXXFLAGS="$CFLAGS"
export LDFLAGS="-march=armv8-a+crc+nosve"
wget https://download.qemu.org/qemu-11.0.0.tar.xz
tar xf qemu-11.0.0.tar.xz
cd qemu-11.0.0
mkdir ../build && cd ../build
../qemu-11.0.0/configure --enable-gtk --enable-sdl --enable-libssh --enable-cap-ng
ninja -j$(nproc)