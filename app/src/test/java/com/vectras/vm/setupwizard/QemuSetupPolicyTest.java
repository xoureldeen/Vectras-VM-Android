package com.vectras.vm.setupwizard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class QemuSetupPolicyTest {
    @Test
    public void arm32AndX86UseAlpinePackagesOnlyForEmptyLinks() {
        assertTrue(QemuSetupPolicy.usesAlpinePackages("armeabi-v7a", ""));
        assertTrue(QemuSetupPolicy.usesAlpinePackages("armeabi", ""));
        assertTrue(QemuSetupPolicy.usesAlpinePackages("x86", "  "));
    }

    @Test
    public void sixtyFourBitAndUnknownAbisKeepArchiveSetup() {
        assertFalse(QemuSetupPolicy.usesAlpinePackages("arm64-v8a", ""));
        assertFalse(QemuSetupPolicy.usesAlpinePackages("x86_64", ""));
        assertFalse(QemuSetupPolicy.usesAlpinePackages("riscv64", ""));
        assertFalse(QemuSetupPolicy.usesAlpinePackages(null, ""));
    }

    @Test
    public void populatedMissingAndInvalidLinksNeverUseAlpinePackages() {
        for (String abi : new String[]{"armeabi-v7a", "armeabi", "x86", "arm64-v8a", "x86_64"}) {
            assertFalse(QemuSetupPolicy.usesAlpinePackages(abi, "https://example.com/qemu.tar.gz"));
            assertFalse(QemuSetupPolicy.usesAlpinePackages(abi, null));
            assertFalse(QemuSetupPolicy.usesAlpinePackages(abi, "not-a-url"));
        }
    }

    @Test
    public void abiMapsToTheCorrectManifestKey() {
        org.junit.Assert.assertEquals("aarch64", QemuSetupPolicy.manifestKey("arm64-v8a"));
        org.junit.Assert.assertEquals("armhf", QemuSetupPolicy.manifestKey("armeabi-v7a"));
        org.junit.Assert.assertEquals("armhf", QemuSetupPolicy.manifestKey("armeabi"));
        org.junit.Assert.assertEquals("amd64", QemuSetupPolicy.manifestKey("x86_64"));
        org.junit.Assert.assertEquals("x86", QemuSetupPolicy.manifestKey("x86"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownAbiDoesNotSelectAnUnrelatedDownload() {
        QemuSetupPolicy.manifestKey("riscv64");
    }

    @Test
    public void packageListIncludesAllAppEnginesToolsAndModules() {
        Set<String> packages = new HashSet<>(Arrays.asList(QemuSetupPolicy.ALPINE_QEMU_PACKAGES.split(" ")));
        for (String required : new String[]{"qemu", "qemu-img", "qemu-tools", "qemu-modules",
                "qemu-system-i386", "qemu-system-x86_64", "qemu-system-aarch64", "qemu-system-ppc", "tigervnc"}) {
            assertTrue("Missing package: " + required, packages.contains(required));
        }
    }
}
