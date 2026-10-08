package com.vectras.vm.setupwizard;

/** Selects the QEMU source for the ABI of the bundled Alpine environment. */
final class QemuSetupPolicy {
    // apk resolves the matching firmware, shared libraries, and module dependencies.
    static final String ALPINE_QEMU_PACKAGES = "qemu qemu-img qemu-tools qemu-modules"
            + " qemu-system-i386 qemu-system-x86_64 qemu-system-aarch64 qemu-system-ppc"
            // Provides vncpasswd for the existing VNC setup step.
            + " tigervnc";

    private QemuSetupPolicy() {
    }

    static boolean is32BitAbi(String abi) {
        return "armeabi-v7a".equals(abi) || "armeabi".equals(abi) || "x86".equals(abi);
    }

    static boolean usesAlpinePackages(String abi, String manifestUrl) {
        // Missing/invalid metadata or a failed request must not count as an empty link.
        return is32BitAbi(abi) && manifestUrl != null && manifestUrl.trim().isEmpty();
    }

    static String manifestKey(String abi) {
        if ("arm64-v8a".equals(abi)) return "aarch64";
        if ("armeabi-v7a".equals(abi) || "armeabi".equals(abi)) return "armhf";
        if ("x86_64".equals(abi)) return "amd64";
        if ("x86".equals(abi)) return "x86";
        throw new IllegalArgumentException("Unsupported QEMU setup ABI: " + abi);
    }
}
