package com.iiordanov.util;

public enum PermissionGroups {
    RECORD_AUDIO("record_audio"),
    RECORD_AND_MODIFY_AUDIO("record_and_modify_audio");

    private final String text;

    PermissionGroups(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
