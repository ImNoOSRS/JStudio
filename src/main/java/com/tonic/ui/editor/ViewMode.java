package com.tonic.ui.editor;

/**
 * View modes for the code editor.
 */
public enum ViewMode {
    SOURCE("Source", "Decompiled Java source code"),
    BYTECODE("Bytecode", "Raw JVM bytecode"),
    IR("IR", "SSA Intermediate Representation"),
    HEX("Hex", "Raw class file bytes");

    private final String displayName;
    private final String description;

    ViewMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
