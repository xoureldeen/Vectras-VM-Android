#!/bin/sh
set -e

# The HTTPS archive URL and optional digest come from the repository setup JSON.
url="$1"
sha256="$2"
archive=/root/setup.tar.gz

echo 'Downloading Qemu...'
if ! curl --fail --location --retry 3 --connect-timeout 30 \
    --proto '=https' --proto-redir '=https' --output "$archive" "$url"; then
    echo 'QEMU download failed. Retry or select a local archive.' >&2
    exit 1
fi

echo 'Verifying QEMU archive...'
if [ -n "$sha256" ]; then
    printf '%s  %s\n' "$sha256" "$archive" | sha256sum -c -
fi
tar -tzf "$archive" >/dev/null
