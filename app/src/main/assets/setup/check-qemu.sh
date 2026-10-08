#!/bin/sh
set -e

# Check the actual installed programs, not just the archive's filename.
command -v qemu-system-x86_64 >/dev/null
command -v qemu-img >/dev/null

needs_libiscsi=false
for binary in /usr/local/bin/qemu-system-* /usr/bin/qemu-system-* /usr/local/bin/qemu-img /usr/bin/qemu-img; do
    [ -f "$binary" ] || continue
    if ldd "$binary" 2>&1 | grep -q 'Error loading shared library libiscsi.so.9'; then
        needs_libiscsi=true
    fi
done

# Some older archives link to libiscsi, absent from Alpine 3.19 main/community.
# Use testing only for this package; do not change the system repositories.
if [ "$needs_libiscsi" = true ]; then
    apk add --repository https://dl-cdn.alpinelinux.org/alpine/edge/testing libiscsi
fi

echo 'Checking QEMU...'
for binary in /usr/local/bin/qemu-system-* /usr/bin/qemu-system-* /usr/local/bin/qemu-img /usr/bin/qemu-img; do
    [ -f "$binary" ] || continue
    "$binary" --version
done
