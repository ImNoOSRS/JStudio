package com.tonic.ui.simulation.model;

/**
 * Base class for all simulation-based analysis findings.
 */
public abstract class SimulationFinding {

    public enum FindingType {
        OPAQUE_PREDICATE,
        DEAD_CODE,
        CONSTANT_VALUE,
        TAINTED_VALUE,
        DECRYPTED_STRING
    }

    public enum Severity {
        INFO,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    protected final String className;
    protected final String methodName;
    protected final String methodDesc;
    protected final FindingType type;
    protected final Severity severity;
    protected final int bytecodeOffset;

    protected SimulationFinding(String className, String methodName, String methodDesc,
                                FindingType type, Severity severity, int bytecodeOffset) {
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.type = type;
        this.severity = severity;
        this.bytecodeOffset = bytecodeOffset;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public FindingType getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public int getBytecodeOffset() {
        return bytecodeOffset;
    }

    public String getMethodSignature() {
        return className + "." + methodName + methodDesc;
    }

    public abstract String getTitle();

    public abstract String getDescription();

    public abstract String getRecommendation();
}
