package kestrel;

import java.util.List;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    private int indent = 0;

    public String print(List<Stmt> statements) {
        StringBuilder sb = new StringBuilder("Program\n");
        indent = 1;
        for (Stmt s : statements) sb.append(s.accept(this)).append("\n");
        return sb.toString().stripTrailing();
    }

    private String pad() { return " ".repeat(indent * 2); }

    // ---- statements ----
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return pad() + "ExpressionStmt\n" + indented(stmt.expression);
    }
    public String visitPrintStmt(Stmt.Print stmt) {
        return pad() + "PrintStmt\n" + indented(stmt.expression);
    }
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder sb = new StringBuilder(pad() + "If\n");
        sb.append(pad()).append("  condition:\n").append(indented(stmt.condition, 2));
        sb.append(pad()).append("  then:\n").append(indentedStmt(stmt.thenBranch, 2));
        if (stmt.elseBranch != null) {
            sb.append(pad()).append("  else:\n").append(indentedStmt(stmt.elseBranch, 2));
        }
        return sb.toString().stripTrailing();
    }
    public String visitWhileStmt(Stmt.While stmt) {
        StringBuilder sb = new StringBuilder(pad() + "While\n");
        sb.append(pad()).append("  condition:\n").append(indented(stmt.condition, 2));
        sb.append(pad()).append("  body:\n").append(indentedStmt(stmt.body, 2));
        return sb.toString().stripTrailing();
    }
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder sb = new StringBuilder(pad() + "Block\n");
        indent++;
        for (Stmt s : stmt.statements) sb.append(s.accept(this)).append("\n");
        indent--;
        return sb.toString().stripTrailing();
    }
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder sb = new StringBuilder(pad() + "Function (" + stmt.name.lexeme + ") params=" + paramList(stmt.params) + "\n");
        indent++;
        sb.append(pad()).append("body:\n");
        indent++;
        for (Stmt s : stmt.body) sb.append(s.accept(this)).append("\n");
        indent -= 2;
        return sb.toString().stripTrailing();
    }
    public String visitReturnStmt(Stmt.Return stmt) {
        StringBuilder sb = new StringBuilder(pad() + "Return\n");
        if (stmt.value != null) sb.append(indented(stmt.value));
        return sb.toString().stripTrailing();
    }

    private String paramList(List<Token> params) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < params.size(); i++) { if (i > 0) sb.append(", "); sb.append(params.get(i).lexeme); }
        return sb.append(")").toString();
    }

    // ---- expressions ----
    public String visitBinaryExpr(Expr.Binary expr) {
        StringBuilder sb = new StringBuilder(pad() + "Binary (" + expr.operator.lexeme + ")\n");
        indent++;
        sb.append(expr.left.accept(this)).append("\n");
        sb.append(expr.right.accept(this));
        indent--;
        return sb.toString();
    }
    public String visitGroupingExpr(Expr.Grouping expr) {
        StringBuilder sb = new StringBuilder(pad() + "Grouping\n");
        indent++;
        sb.append(expr.expression.accept(this));
        indent--;
        return sb.toString();
    }
    public String visitLiteralExpr(Expr.Literal expr) {
        Object v = expr.value;
        String rendered = (v instanceof String s) ? "\"" + s + "\"" : String.valueOf(v);
        return pad() + "Literal (" + rendered + ")";
    }
    public String visitUnaryExpr(Expr.Unary expr) {
        StringBuilder sb = new StringBuilder(pad() + "Unary (" + expr.operator.lexeme + ")\n");
        indent++;
        sb.append(expr.right.accept(this));
        indent--;
        return sb.toString();
    }
    public String visitVariableExpr(Expr.Variable expr) {
        return pad() + "Variable (" + expr.name.lexeme + ")";
    }
    public String visitAssignExpr(Expr.Assign expr) {
        StringBuilder sb = new StringBuilder(pad() + "Assign (" + expr.name.lexeme + ")\n");
        indent++;
        sb.append(expr.value.accept(this));
        indent--;
        return sb.toString();
    }
    public String visitLogicalExpr(Expr.Logical expr) {
        StringBuilder sb = new StringBuilder(pad() + "Logical (" + expr.operator.lexeme + ")\n");
        indent++;
        sb.append(expr.left.accept(this)).append("\n");
        sb.append(expr.right.accept(this));
        indent--;
        return sb.toString();
    }
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder sb = new StringBuilder(pad() + "Call\n");
        indent++;
        sb.append(pad()).append("callee:\n").append(indented(expr.callee, 1));
        if (!expr.arguments.isEmpty()) {
            sb.append(pad()).append("args:\n");
            indent++;
            for (Expr a : expr.arguments) sb.append(a.accept(this)).append("\n");
            indent--;
        }
        indent--;
        return sb.toString().stripTrailing();
    }

    private String indented(Expr e) { return indented(e, 1); }
    private String indented(Expr e, int levels) {
        indent += levels;
        String s = e.accept(this) + "\n";
        indent -= levels;
        return s;
    }
    private String indentedStmt(Stmt s, int levels) {
        indent += levels;
        String out = s.accept(this) + "\n";
        indent -= levels;
        return out;
    }
}
