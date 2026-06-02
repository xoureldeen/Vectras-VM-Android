#!/bin/bash
clear
unset qemuurl
architecture=$(uname -m)
if [[ "$architecture" =~ "aarch64" ]]; then
    qemuurl="https://github.com/AnBui2004/Vectras-VM-Emu-Android/releases/download/4.1.1/base-may-2026-vectras-vm-arm64-v8a.tar.gz"
elif [[ "$architecture" =~ "x86_64" ]]; then
    qemuurl="https://github.com/AnBui2004/Vectras-VM-Emu-Android/releases/download/3.5.1/base-generic-vectras-vm-x86_64.tar.gz"
elif [[ "$architecture" =~ "arm" ]]; then
    qemuurl="https://github.com/AnBui2004/Vectras-VM-Emu-Android/releases/download/3.5.4/base-vectras-vm-armeabi-v7a.tar.gz"
elif [[ "$architecture" =~ "x86" ]]; then
    qemuurl="https://github.com/AnBui2004/Vectras-VM-Emu-Android/releases/download/3.5.4/base-vectras-vm-x86.tar.gz"
elif [[ "$architecture" =~ "i386" || "$architecture" =~ "i686" ]]; then
    qemuurl="https://github.com/AnBui2004/Vectras-VM-Emu-Android/releases/download/3.5.4/base-vectras-vm-x86.tar.gz"
fi

if [[ -z "$qemuurl" ]]; then
    echo -e "\e[1;37m[!] Unsupported architecture!"
    echo -e "\e[1;37m-\e[0m"
    echo -e "\e[1;37mYour device's is not supported."
    echo -e "\e[1;37m-\e[0m"
    echo -e "\e[1;37mSetup was canceled."
    rm -f setup.sh
    exit
fi

echo -e "\e[1;37m[!] Qemu 9.2.2 upgrade tool. Warning and do not ignore!"
echo -e "\e[1;37m-\e[0m"
echo -e "\e[1;37mPlease do not run any other commands when this setup begins. If you're running other commands, they haven't finished executing yet or don't want some packages to be forced to be updated when setting up. Any existing installed version of Qemu will be uninstalled. Press Ctrl + C now to cancel the setup immediately."
echo -e "\e[1;37m\e[0m"
echo -e "\e[1;37mBy using Qemu in any way, you agree to the terms, policies, and other related provisions. The owner of this script is not responsible for any consequences that may arise from using Qemu or you have edited these scripts. To disagree and cancel the setup, press Ctrl + C."
echo -e "\e[1;37m-\e[0m"
echo -e "\e[1;37mAutomatically go to next step after 60 seconds or continue immediately by pressing any key and you agree to the above."
if read -r -t 60 -n 1 _; then
    echo "Pressed the key and continued immediately."
else
    echo "60 seconds elapsed, auto continue."
fi
clear

echo -e "\e[1;37m[i] Killing process..."
pkill -15 -f qemu-system- || true
sleep 1
pkill -9 -f qemu-system- || true
clear

echo -e "\e[1;37m[i] Uninstalling current Qemu..."
rm -f /usr/local/bin/qemu-*
rm -f /usr/share/applications/qemu.desktop
rm -f /usr/share/icons/hicolor/*/qemu.png
rm -rf /usr/share/qemu
clear

echo -e "\e[1;37m[i] Downloading..."
aria2c -x 4 --async-dns=false --disable-ipv6 --check-certificate=false -o setup.tar.gz "$qemuurl"
clear

echo -e "\e[1;37m[i] Installing..."
tar -xzvf setup.tar.gz -C /
clear

echo -e "\e[1;37m[i] Just a sec..."
rm -f setup.tar.gz
chmod 775 /usr/local/bin/qemu*
clear

echo -e "\e[1;37m[i] Done!"
qemu-system-x86_64 -version
rm -f setup.sh