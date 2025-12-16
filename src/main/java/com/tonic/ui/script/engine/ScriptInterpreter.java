package com.tonic.ui.script.engine;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interpreter for JStudio script language.
 */
public class ScriptInterpreter implements ScriptAST.Visitor<ScriptValue> {

    @Getter
    private final ScriptContext globalContext;

    @Getter
    private ScriptContext currentContext;

    private final List<String> logs = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private Consumer<String> logCallback;
    private Consumer<String> warnCallback;
    private Consumer<String> errorCallback;

    // Used for return statements
    private ScriptValue returnValue = null;
    private boolean returning = false;

    public ScriptInterpreter() {
        this.globalContext = new ScriptContext();
        this.currentContext = globalContext;
        registerBuiltins();
    }

    private void registerBuiltins() {
        // Logging functions
        globalContext.defineConstant("log", ScriptValue.function(
            ScriptFunction.nativeN("log", args -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(args.get(i).asString());
                }
                log(sb.toString());
                return ScriptValue.NULL;
            })
        ));

        globalContext.defineConstant("warn", ScriptValue.function(
            ScriptFunction.native1("warn", arg -> {
                warn(arg.asString());
                return ScriptValue.NULL;
            })
        ));

        globalContext.defineConstant("error", ScriptValue.function(
            ScriptFunction.native1("error", arg -> {
                error(arg.asString());
                return ScriptValue.NULL;
            })
        ));

        // String functions
        globalContext.defineConstant("String", createStringObject());

        // Utility functions
        globalContext.defineConstant("typeof", ScriptValue.function(
            ScriptFunction.native1("typeof", arg -> {
                return ScriptValue.string(arg.getType().name().toLowerCase());
            })
        ));

        globalContext.defineConstant("parseInt", ScriptValue.function(
            ScriptFunction.native1("parseInt", arg -> {
                try {
                    return ScriptValue.number((int) Double.parseDouble(arg.asString()));
                } catch (NumberFormatException e) {
                    return ScriptValue.number(Double.NaN);
                }
            })
        ));

        globalContext.defineConstant("parseFloat", ScriptValue.function(
            ScriptFunction.native1("parseFloat", arg -> {
                try {
                    return ScriptValue.number(Double.parseDouble(arg.asString()));
                } catch (NumberFormatException e) {
                    return ScriptValue.number(Double.NaN);
                }
            })
        ));
    }

    private ScriptValue createStringObject() {
        java.util.Map<String, ScriptValue> props = new java.util.HashMap<>();

        props.put("fromCharCode", ScriptValue.function(
            ScriptFunction.native1("fromCharCode", arg -> {
                int code = (int) arg.asNumber();
                return ScriptValue.string(String.valueOf((char) code));
            })
        ));

        return ScriptValue.object(props);
    }

    // ==================== Execution ====================

    public ScriptValue execute(List<ScriptAST> statements) {
        ScriptValue result = ScriptValue.NULL;

        for (ScriptAST stmt : statements) {
            result = evaluate(stmt);
            if (returning) {
                returning = false;
                return returnValue;
            }
        }

        return result;
    }

    public ScriptValue executeInContext(ScriptAST node, ScriptContext context) {
        ScriptContext previous = currentContext;
        currentContext = context;
        try {
            if (node instanceof ScriptAST.BlockStmt) {
                return execute(((ScriptAST.BlockStmt) node).getStatements());
            } else {
                // Expression body - return its value
                return evaluate(node);
            }
        } finally {
            currentContext = previous;
        }
    }

    private ScriptValue evaluate(ScriptAST node) {
        if (returning) return ScriptValue.NULL;
        return node.accept(this);
    }

    // ==================== Visitors ====================

    @Override
    public ScriptValue visitLiteral(ScriptAST.LiteralExpr expr) {
        return ScriptValue.of(expr.getValue());
    }

    @Override
    public ScriptValue visitIdentifier(ScriptAST.IdentifierExpr expr) {
        return currentContext.get(expr.getName());
    }

    @Override
    public ScriptValue visitBinary(ScriptAST.BinaryExpr expr) {
        String op = expr.getOperator();

        // Short-circuit evaluation for && and ||
        if ("&&".equals(op)) {
            ScriptValue left = evaluate(expr.getLeft());
            if (!left.asBoolean()) return ScriptValue.FALSE;
            return ScriptValue.bool(evaluate(expr.getRight()).asBoolean());
        }

        if ("||".equals(op)) {
            ScriptValue left = evaluate(expr.getLeft());
            if (left.asBoolean()) return left;
            return evaluate(expr.getRight());
        }

        // Assignment
        if ("=".equals(op)) {
            ScriptValue value = evaluate(expr.getRight());

            if (expr.getLeft() instanceof ScriptAST.IdentifierExpr) {
                String name = ((ScriptAST.IdentifierExpr) expr.getLeft()).getName();
                currentContext.set(name, value);
            } else if (expr.getLeft() instanceof ScriptAST.MemberAccessExpr) {
                ScriptAST.MemberAccessExpr member = (ScriptAST.MemberAccessExpr) expr.getLeft();
                ScriptValue obj = evaluate(member.getObject());
                obj.setProperty(member.getMember(), value);
            }

            return value;
        }

        ScriptValue left = evaluate(expr.getLeft());
        ScriptValue right = evaluate(expr.getRight());

        switch (op) {
            case "+": return ScriptValue.add(left, right);
            case "-": return ScriptValue.subtract(left, right);
            case "*": return ScriptValue.multiply(left, right);
            case "/": return ScriptValue.divide(left, right);
            case "%": return ScriptValue.modulo(left, right);
            case "==": return ScriptValue.bool(ScriptValue.equals(left, right));
            case "!=": return ScriptValue.bool(!ScriptValue.equals(left, right));
            case "<": return ScriptValue.bool(ScriptValue.compare(left, right) < 0);
            case "<=": return ScriptValue.bool(ScriptValue.compare(left, right) <= 0);
            case ">": return ScriptValue.bool(ScriptValue.compare(left, right) > 0);
            case ">=": return ScriptValue.bool(ScriptValue.compare(left, right) >= 0);
            default:
                throw new RuntimeException("Unknown binary operator: " + op);
        }
    }

    @Override
    public ScriptValue visitUnary(ScriptAST.UnaryExpr expr) {
        ScriptValue operand = evaluate(expr.getOperand());

        switch (expr.getOperator()) {
            case "-": return ScriptValue.negate(operand);
            case "!": return ScriptValue.not(operand);
            default:
                throw new RuntimeException("Unknown unary operator: " + expr.getOperator());
        }
    }

    @Override
    public ScriptValue visitCall(ScriptAST.CallExpr expr) {
        ScriptValue callee = evaluate(expr.getCallee());

        if (!callee.isFunction()) {
            throw new RuntimeException("Cannot call non-function: " + callee);
        }

        List<ScriptValue> args = new ArrayList<>();
        for (ScriptAST arg : expr.getArguments()) {
            args.add(evaluate(arg));
        }

        ScriptFunction func = callee.asFunction();
        return func.call(this, args);
    }

    @Override
    public ScriptValue visitMemberAccess(ScriptAST.MemberAccessExpr expr) {
        ScriptValue obj = evaluate(expr.getObject());

        // Optional chaining
        if (expr.isOptional() && obj.isNull()) {
            return ScriptValue.NULL;
        }

        // Check for method calls on native string
        if (obj.isString()) {
            return getStringMethod(obj, expr.getMember());
        }

        return obj.getProperty(expr.getMember());
    }

    private ScriptValue getStringMethod(ScriptValue strVal, String method) {
        String s = strVal.asString();

        switch (method) {
            case "length":
                return ScriptValue.number(s.length());
            case "toLowerCase":
                return ScriptValue.function(ScriptFunction.native0("toLowerCase",
                    () -> ScriptValue.string(s.toLowerCase())));
            case "toUpperCase":
                return ScriptValue.function(ScriptFunction.native0("toUpperCase",
                    () -> ScriptValue.string(s.toUpperCase())));
            case "trim":
                return ScriptValue.function(ScriptFunction.native0("trim",
                    () -> ScriptValue.string(s.trim())));
            case "startsWith":
                return ScriptValue.function(ScriptFunction.native1("startsWith",
                    arg -> ScriptValue.bool(s.startsWith(arg.asString()))));
            case "endsWith":
                return ScriptValue.function(ScriptFunction.native1("endsWith",
                    arg -> ScriptValue.bool(s.endsWith(arg.asString()))));
            case "includes":
            case "contains":
                return ScriptValue.function(ScriptFunction.native1("includes",
                    arg -> ScriptValue.bool(s.contains(arg.asString()))));
            case "indexOf":
                return ScriptValue.function(ScriptFunction.native1("indexOf",
                    arg -> ScriptValue.number(s.indexOf(arg.asString()))));
            case "substring":
                return ScriptValue.function(ScriptFunction.native2("substring",
                    (start, end) -> {
                        int startIdx = (int) start.asNumber();
                        if (end.isNull()) {
                            return ScriptValue.string(s.substring(startIdx));
                        }
                        int endIdx = (int) end.asNumber();
                        return ScriptValue.string(s.substring(startIdx, endIdx));
                    }));
            case "replace":
                return ScriptValue.function(ScriptFunction.native2("replace",
                    (search, replacement) ->
                        ScriptValue.string(s.replace(search.asString(), replacement.asString()))));
            case "split":
                return ScriptValue.function(ScriptFunction.native1("split",
                    arg -> {
                        String[] parts = s.split(java.util.regex.Pattern.quote(arg.asString()));
                        List<ScriptValue> list = new ArrayList<>();
                        for (String part : parts) {
                            list.add(ScriptValue.string(part));
                        }
                        return ScriptValue.array(list);
                    }));
            default:
                return ScriptValue.NULL;
        }
    }

    @Override
    public ScriptValue visitArrowFunction(ScriptAST.ArrowFunctionExpr expr) {
        return ScriptValue.function(new ScriptFunction.UserFunction(
            expr.getParameters(),
            expr.getBody(),
            currentContext
        ));
    }

    @Override
    public ScriptValue visitArrayAccess(ScriptAST.ArrayAccessExpr expr) {
        ScriptValue arr = evaluate(expr.getArray());
        ScriptValue idx = evaluate(expr.getIndex());

        if (arr.isArray()) {
            int index = (int) idx.asNumber();
            List<ScriptValue> list = arr.asArray();
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
            return ScriptValue.NULL;
        }

        if (arr.isString()) {
            int index = (int) idx.asNumber();
            String s = arr.asString();
            if (index >= 0 && index < s.length()) {
                return ScriptValue.string(String.valueOf(s.charAt(index)));
            }
            return ScriptValue.NULL;
        }

        if (arr.isObject()) {
            return arr.getProperty(idx.asString());
        }

        return ScriptValue.NULL;
    }

    @Override
    public ScriptValue visitTernary(ScriptAST.TernaryExpr expr) {
        ScriptValue condition = evaluate(expr.getCondition());
        if (condition.asBoolean()) {
            return evaluate(expr.getThenBranch());
        } else {
            return evaluate(expr.getElseBranch());
        }
    }

    @Override
    public ScriptValue visitExpressionStmt(ScriptAST.ExpressionStmt stmt) {
        return evaluate(stmt.getExpression());
    }

    @Override
    public ScriptValue visitVarDecl(ScriptAST.VarDeclStmt stmt) {
        ScriptValue value = stmt.getInitializer() != null
            ? evaluate(stmt.getInitializer())
            : ScriptValue.NULL;

        if (stmt.isConstant()) {
            currentContext.defineConstant(stmt.getName(), value);
        } else {
            currentContext.define(stmt.getName(), value);
        }

        return value;
    }

    @Override
    public ScriptValue visitIf(ScriptAST.IfStmt stmt) {
        ScriptValue condition = evaluate(stmt.getCondition());

        if (condition.asBoolean()) {
            return evaluate(stmt.getThenBranch());
        } else if (stmt.getElseBranch() != null) {
            return evaluate(stmt.getElseBranch());
        }

        return ScriptValue.NULL;
    }

    @Override
    public ScriptValue visitReturn(ScriptAST.ReturnStmt stmt) {
        returnValue = stmt.getValue() != null
            ? evaluate(stmt.getValue())
            : ScriptValue.NULL;
        returning = true;
        return returnValue;
    }

    @Override
    public ScriptValue visitBlock(ScriptAST.BlockStmt stmt) {
        ScriptContext previous = currentContext;
        currentContext = currentContext.child();
        try {
            return execute(stmt.getStatements());
        } finally {
            currentContext = previous;
        }
    }

    // ==================== Logging ====================

    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    public void setWarnCallback(Consumer<String> callback) {
        this.warnCallback = callback;
    }

    public void setErrorCallback(Consumer<String> callback) {
        this.errorCallback = callback;
    }

    private void log(String message) {
        logs.add(message);
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }

    private void warn(String message) {
        warnings.add(message);
        if (warnCallback != null) {
            warnCallback.accept("WARN: " + message);
        }
    }

    private void error(String message) {
        errors.add(message);
        if (errorCallback != null) {
            errorCallback.accept("ERROR: " + message);
        }
    }

    public List<String> getLogs() {
        return logs;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void clearLogs() {
        logs.clear();
        warnings.clear();
        errors.clear();
    }
}
