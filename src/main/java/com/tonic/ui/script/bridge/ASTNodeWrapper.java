package com.tonic.ui.script.bridge;

import com.tonic.analysis.source.ast.expr.*;
import com.tonic.analysis.source.ast.stmt.*;
import com.tonic.ui.script.engine.ScriptValue;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps AST nodes for script access.
 * Provides a uniform interface to access node properties from scripts.
 */
@Getter
public class ASTNodeWrapper {

    private final Object node;

    public ASTNodeWrapper(Object node) {
        this.node = node;
    }

    public Object unwrap() {
        return node;
    }

    /**
     * Creates a ScriptValue representing this node.
     */
    public ScriptValue toScriptValue() {
        Map<String, ScriptValue> props = new HashMap<>();

        // Common properties
        props.put("type", ScriptValue.string(node.getClass().getSimpleName()));
        props.put("_native", ScriptValue.native_(this));

        // Type-specific properties
        if (node instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) node;
            props.put("name", ScriptValue.string(call.getMethodName()));
            props.put("owner", ScriptValue.string(call.getOwnerClass() != null ? call.getOwnerClass() : ""));
            props.put("receiver", call.getReceiver() != null ?
                new ASTNodeWrapper(call.getReceiver()).toScriptValue() : ScriptValue.NULL);
            props.put("args", wrapList(call.getArguments()));
            props.put("isStatic", ScriptValue.bool(call.isStatic()));
        }
        else if (node instanceof FieldAccessExpr) {
            FieldAccessExpr field = (FieldAccessExpr) node;
            props.put("name", ScriptValue.string(field.getFieldName()));
            props.put("owner", ScriptValue.string(field.getOwnerClass() != null ? field.getOwnerClass() : ""));
            props.put("receiver", field.getReceiver() != null ?
                new ASTNodeWrapper(field.getReceiver()).toScriptValue() : ScriptValue.NULL);
            props.put("isStatic", ScriptValue.bool(field.isStatic()));
        }
        else if (node instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) node;
            props.put("op", ScriptValue.string(binary.getOperator().getSymbol()));
            props.put("left", new ASTNodeWrapper(binary.getLeft()).toScriptValue());
            props.put("right", new ASTNodeWrapper(binary.getRight()).toScriptValue());
            props.put("isComparison", ScriptValue.bool(binary.isComparison()));
            props.put("isLogical", ScriptValue.bool(binary.isLogical()));
            props.put("isAssignment", ScriptValue.bool(binary.isAssignment()));
        }
        else if (node instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) node;
            props.put("op", ScriptValue.string(unary.getOperator().getSymbol()));
            props.put("operand", new ASTNodeWrapper(unary.getOperand()).toScriptValue());
            props.put("isPrefix", ScriptValue.bool(unary.isPrefix()));
        }
        else if (node instanceof LiteralExpr) {
            LiteralExpr lit = (LiteralExpr) node;
            props.put("value", wrapLiteralValue(lit.getValue()));
            props.put("isNull", ScriptValue.bool(lit.isNull()));
            props.put("isString", ScriptValue.bool(lit.isString()));
            props.put("isNumeric", ScriptValue.bool(lit.isNumeric()));
            props.put("isConstant", ScriptValue.TRUE);
        }
        else if (node instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) node;
            props.put("condition", new ASTNodeWrapper(ifStmt.getCondition()).toScriptValue());
            props.put("thenBranch", new ASTNodeWrapper(ifStmt.getThenBranch()).toScriptValue());
            props.put("elseBranch", ifStmt.getElseBranch() != null ?
                new ASTNodeWrapper(ifStmt.getElseBranch()).toScriptValue() : ScriptValue.NULL);
        }
        else if (node instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt) node;
            props.put("condition", new ASTNodeWrapper(whileStmt.getCondition()).toScriptValue());
            props.put("body", new ASTNodeWrapper(whileStmt.getBody()).toScriptValue());
        }
        else if (node instanceof ReturnStmt) {
            ReturnStmt ret = (ReturnStmt) node;
            props.put("value", ret.getValue() != null ?
                new ASTNodeWrapper(ret.getValue()).toScriptValue() : ScriptValue.NULL);
        }
        else if (node instanceof BlockStmt) {
            BlockStmt block = (BlockStmt) node;
            props.put("statements", wrapList(block.getStatements()));
        }

        return ScriptValue.object(props);
    }

    private ScriptValue wrapLiteralValue(Object value) {
        if (value == null) return ScriptValue.NULL;
        if (value instanceof String) return ScriptValue.string((String) value);
        if (value instanceof Number) return ScriptValue.number(((Number) value).doubleValue());
        if (value instanceof Boolean) return ScriptValue.bool((Boolean) value);
        if (value instanceof Character) return ScriptValue.string(String.valueOf(value));
        return ScriptValue.string(value.toString());
    }

    private ScriptValue wrapList(List<?> items) {
        List<ScriptValue> wrapped = new ArrayList<>();
        for (Object item : items) {
            wrapped.add(new ASTNodeWrapper(item).toScriptValue());
        }
        return ScriptValue.array(wrapped);
    }
}
