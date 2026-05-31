#Example for arm64-v8a
apk update
apk add --no-cache bash curl tar rsync flex build-base git meson bison ninja python3 py3-pip libcap-ng-dev glib-dev pixman-dev sdl2-dev sdl2_image-dev sndio alsa-utils alsaconf zlib-dev libaio-dev liburing-dev libcap libcap-ng libssh lzo snappy capstone libcbor libdw gtk+3.0-dev libssh-dev libnfs-dev libseccomp-dev lzo-dev snappy-dev capstone-dev ndctl-libs libcbor-dev libselinux libselinux-dev fuse-dev vde2-dev nmap sndio-dev pipewire-dev alsa-lib-dev vte3-dev keyutils keyutils-dev rng-tools linux-virt nettle-dev libgcrypt libgcrypt-dev gnutls-dev iasl gcc-objc rust libudev-zero-dev ndctl-dev libu2f-server libu2f-server-dev libbpf libbpf-dev rdma-core-openrc curl-dev linux-pam linux-pam-dev net-snmp-dev jack-dev fuse3 fuse3-dev nano perl-dev
pip install Ninja Sphinx --break-system-packages
pip install meson --upgrade --break-system-packages
pip install sphinx-rtd-theme --break-system-packages
export CFLAGS="-O2 -march=armv8-a+crc+nosve -D_LARGEFILE64_SOURCE"
export CXXFLAGS="$CFLAGS"
export LDFLAGS="-march=armv8-a+crc+nosve"
mkdir myqemu && cd myqemu
git clone https://github.com/kjliew/qemu-3dfx.git
cd qemu-3dfx
wget https://download.qemu.org/qemu-7.2.22.tar.xz
tar xf qemu-7.2.22.tar.xz
cd qemu-7.2.22
rsync -r ../qemu-0/hw/3dfx ../qemu-1/hw/mesa ./hw/
patch -p0 -i ../02-qemu72x-mesa-glide.patch
bash ../scripts/sign_commit
mkdir ../build && cd ../build
../qemu-7.2.22/configure --enable-gtk --enable-sdl --enable-libssh --enable-cap-ng --enable-lto
ninja -j$(nproc)