package kestrel;

import java.util.ArrayList;
import java.util.List;

/**
 * Second tree-walking pass, run after a syntactically valid AST has been produced.
 * Populates the SymbolTable while resolving every identifier, validates function-call
 * arity, checks return-statement placement, and performs conservative static type
 * checks. Errors are accumulated rather than thrown, so one bad statement does not
 * prevent the rest of the program from being checked (structured error recovery).
 */
public class SemanticAnalyzer implements Stmt.Visitor<Void>, Expr.Visitor<Symbol.Type> {
    public final SymbolTable table = new SymbolTable();
    public final List<CompilerError> errors = new ArrayList<>();
    private int functionDepth = 0;
    private int blockCounter = 0;

    public void analyze(List<Stmt> statements) {
        for (Stmt s : statements) s.accept(this);
    }

    // ---------------- statements ----------------

    @Override public Void visitExpressionStmt(Stmt.Expression stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override public Void visitPrintStmt(Stmt.Print stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override public Void visitIfStmt(Stmt.If stmt) {
        Symbol.Type condType = stmt.condition.accept(this);
        warnIfNotBoolean(condType, "if condition", exprLine(stmt.condition));
        stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
        return null;
    }

    @Override public Void visitWhileStmt(Stmt.While stmt) {
        Symbol.Type condType = stmt.condition.accept(this);
        warnIfNotBoolean(condType, "while condition", exprLine(stmt.condition));
        stmt.body.accept(this);
        return null;
    }

    @Override public Void visitBlockStmt(Stmt.Block stmt) {
        table.pushScope(Scope.Kind.BLOCK, "block#" + (++blockCounter));
        for (Stmt s : stmt.statements) s.accept(this);
        table.popScope();
        return null;
    }

    @Override public Void visitFunctionStmt(Stmt.Function stmt) {
        // Register the function in the *enclosing* scope before analyzing its body,
        // so that a recursive call to itself resolves correctly.
        Symbol fnSymbol = new Symbol(stmt.name.lexeme, Symbol.Type.FUNCTION, stmt.params.size());
        if (table.definedInCurrentScope(stmt.name.lexeme)) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, stmt.name.line,
                    "function '" + stmt.name.lexeme + "' redeclared in the same scope"));
        }
        table.define(fnSymbol);

        table.pushScope(Scope.Kind.FUNCTION, stmt.name.lexeme);
        for (Token param : stmt.params) {
            table.define(new Symbol(param.lexeme, Symbol.Type.UNKNOWN));
        }
        functionDepth++;
        for (Stmt s : stmt.body) s.accept(this);
        functionDepth--;
        table.popScope();
        return null;
    }

    @Override public Void visitReturnStmt(Stmt.Return stmt) {
        if (functionDepth == 0) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, stmt.keyword.line,
                    "'return' used outside of a function"));
        }
        if (stmt.value != null) stmt.value.accept(this);
        return null;
    }

    // ---------------- expressions ----------------

    @Override public Symbol.Type visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof Double) return Symbol.Type.NUMBER;
        if (expr.value instanceof String) return Symbol.Type.STRING;
        if (expr.value instanceof Boolean) return Symbol.Type.BOOLEAN;
        return Symbol.Type.UNKNOWN;
    }

    @Override public Symbol.Type visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override public Symbol.Type visitVariableExpr(Expr.Variable expr) {
        Symbol s = table.resolve(expr.name.lexeme);
        if (s == null) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, expr.name.line,
                    "undeclared variable '" + expr.name.lexeme + "'"));
            return Symbol.Type.UNKNOWN;
        }
        return s.type;
    }

    @Override public Symbol.Type visitAssignExpr(Expr.Assign expr) {
        Symbol.Type valueType = expr.value.accept(this);
        Symbol existing = table.resolveForAssignment(expr.name.lexeme);
        if (existing == null) {
            // Not visible from an assignment within the current function (or, at global
            // scope, not visible at all): declare it fresh in the *current* (innermost)
            // scope. Inside a function this creates a genuine local that shadows any
            // same-named global; inside a nested block it scopes the variable to that block.
            table.define(new Symbol(expr.name.lexeme, valueType));
        } else if (existing.type == Symbol.Type.FUNCTION) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, expr.name.line,
                    "cannot assign to '" + expr.name.lexeme + "': it is a function"));
        } else {
            existing.type = valueType; // refine inferred type on reassignment
        }
        return valueType;
    }

    @Override public Symbol.Type visitUnaryExpr(Expr.Unary expr) {
        Symbol.Type t = expr.right.accept(this);
        if (expr.operator.type == TokenType.MINUS) {
            if (t != Symbol.Type.UNKNOWN && t != Symbol.Type.NUMBER) {
                errors.add(typeError(expr.operator.line, "unary '-' requires a NUMBER operand, got " + t));
            }
            return Symbol.Type.NUMBER;
        } else { // BANG
            if (t != Symbol.Type.UNKNOWN && t != Symbol.Type.BOOLEAN) {
                errors.add(typeError(expr.operator.line, "unary '!' requires a BOOLEAN operand, got " + t));
            }
            return Symbol.Type.BOOLEAN;
        }
    }

    @Override public Symbol.Type visitLogicalExpr(Expr.Logical expr) {
        Symbol.Type l = expr.left.accept(this);
        Symbol.Type r = expr.right.accept(this);
        warnIfNotBoolean(l, "left operand of '" + expr.operator.lexeme + "'", expr.operator.line);
        warnIfNotBoolean(r, "right operand of '" + expr.operator.lexeme + "'", expr.operator.line);
        return Symbol.Type.BOOLEAN;
    }

    @Override public Symbol.Type visitBinaryExpr(Expr.Binary expr) {
        Symbol.Type l = expr.left.accept(this);
        Symbol.Type r = expr.right.accept(this);
        String op = expr.operator.lexeme;
        switch (expr.operator.type) {
            case PLUS -> {
                boolean bothNumber = l == Symbol.Type.NUMBER && r == Symbol.Type.NUMBER;
                boolean bothString = l == Symbol.Type.STRING && r == Symbol.Type.STRING;
                boolean eitherUnknown = l == Symbol.Type.UNKNOWN || r == Symbol.Type.UNKNOWN;
                if (!bothNumber && !bothString && !eitherUnknown) {
                    errors.add(typeError(expr.operator.line, "'+' requires two NUMBERs or two STRINGs, got " + l + " and " + r));
                }
                return bothString ? Symbol.Type.STRING : Symbol.Type.NUMBER;
            }
            case MINUS, STAR, SLASH -> {
                boolean eitherUnknown = l == Symbol.Type.UNKNOWN || r == Symbol.Type.UNKNOWN;
                if (!eitherUnknown && (l != Symbol.Type.NUMBER || r != Symbol.Type.NUMBER)) {
                    errors.add(typeError(expr.operator.line, "'" + op + "' requires NUMBER operands, got " + l + " and " + r));
                }
                return Symbol.Type.NUMBER;
            }
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> {
                boolean eitherUnknown = l == Symbol.Type.UNKNOWN || r == Symbol.Type.UNKNOWN;
                if (!eitherUnknown && (l != Symbol.Type.NUMBER || r != Symbol.Type.NUMBER)) {
                    errors.add(typeError(expr.operator.line, "'" + op + "' requires NUMBER operands, got " + l + " and " + r));
                }
                return Symbol.Type.BOOLEAN;
            }
            default -> { // == !=
                return Symbol.Type.BOOLEAN;
            }
        }
    }

    @Override public Symbol.Type visitCallExpr(Expr.Call expr) {
        if (!(expr.callee instanceof Expr.Variable v)) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, expr.paren.line,
                    "only direct function names can be called in this phase"));
            for (Expr a : expr.arguments) a.accept(this);
            return Symbol.Type.UNKNOWN;
        }
        Symbol fn = table.resolve(v.name.lexeme);
        for (Expr a : expr.arguments) a.accept(this); // still check argument expressions even on failure
        if (fn == null) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, expr.paren.line,
                    "call to undeclared function '" + v.name.lexeme + "'"));
            return Symbol.Type.UNKNOWN;
        }
        if (fn.type != Symbol.Type.FUNCTION) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, expr.paren.line,
                    "'" + v.name.lexeme + "' is not a function"));
            return Symbol.Type.UNKNOWN;
        }
        if (fn.arity != expr.arguments.size()) {
            errors.add(new CompilerError(CompilerError.Stage.SEMANTIC, expr.paren.line,
                    "function '" + v.name.lexeme + "' expects " + fn.arity + " argument(s) but got " + expr.arguments.size()));
        }
        // Return types are not tracked in Phase 2, so a call expression's static type is UNKNOWN.
        return Symbol.Type.UNKNOWN;
    }

    // ---------------- helpers ----------------

    private void warnIfNotBoolean(Symbol.Type t, String where, int line) {
        if (t != Symbol.Type.UNKNOWN && t != Symbol.Type.BOOLEAN) {
            errors.add(typeError(line, where + " should be BOOLEAN, got " + t));
        }
    }

    private int exprLine(Expr e) {
        if (e instanceof Expr.Variable v) return v.name.line;
        if (e instanceof Expr.Assign a) return a.name.line;
        if (e instanceof Expr.Binary b) return b.operator.line;
        if (e instanceof Expr.Logical l) return l.operator.line;
        if (e instanceof Expr.Unary u) return u.operator.line;
        if (e instanceof Expr.Call c) return c.paren.line;
        if (e instanceof Expr.Grouping g) return exprLine(g.expression);
        return 0;
    }

    private CompilerError typeError(int line, String message) {
        return new CompilerError(CompilerError.Stage.SEMANTIC, line, message);
    }
}
