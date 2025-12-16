package com.tonic.ui.event.events;

import com.tonic.ui.event.Event;

/**
 * Fired when cross-references should be shown for a symbol.
 * Can be triggered by:
 * - Context menu "Show Cross-References"
 * - Double-clicking a reference count in the gutter
 * - Keyboard shortcut
 */
public class ShowXrefsEvent extends Event {

    /**
     * Type of symbol to show xrefs for.
     */
    public enum TargetType {
        CLASS,
        METHOD,
        FIELD
    }

    private final TargetType targetType;
    private final String className;
    private final String memberName;      // null for class-level xrefs
    private final String memberDescriptor; // null for class-level xrefs

    /**
     * Create an event to show xrefs for a class.
     */
    public ShowXrefsEvent(Object source, String className) {
        super(source);
        this.targetType = TargetType.CLASS;
        this.className = className;
        this.memberName = null;
        this.memberDescriptor = null;
    }

    /**
     * Create an event to show xrefs for a method.
     */
    public static ShowXrefsEvent forMethod(Object source, String className,
                                            String methodName, String methodDesc) {
        return new ShowXrefsEvent(source, TargetType.METHOD, className, methodName, methodDesc);
    }

    /**
     * Create an event to show xrefs for a field.
     */
    public static ShowXrefsEvent forField(Object source, String className,
                                           String fieldName, String fieldDesc) {
        return new ShowXrefsEvent(source, TargetType.FIELD, className, fieldName, fieldDesc);
    }

    private ShowXrefsEvent(Object source, TargetType targetType, String className,
                           String memberName, String memberDescriptor) {
        super(source);
        this.targetType = targetType;
        this.className = className;
        this.memberName = memberName;
        this.memberDescriptor = memberDescriptor;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public String getClassName() {
        return className;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getMemberDescriptor() {
        return memberDescriptor;
    }

    /**
     * Get a display string for the target.
     */
    public String getTargetDisplay() {
        String displayClass = className != null ? className.replace('/', '.') : "unknown";
        switch (targetType) {
            case CLASS:
                return displayClass;
            case METHOD:
                return displayClass + "." + memberName + "()";
            case FIELD:
                return displayClass + "." + memberName;
            default:
                return displayClass;
        }
    }
}
