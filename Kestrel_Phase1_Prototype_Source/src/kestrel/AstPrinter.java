package kestrel;

import java.util.List;

/**
 * Renders the AST produced by the Parser as a human-readable indented tree.
 * Implements both Expr.Visitor and Stmt.Visitor so it can walk the whole program.
 */
public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    private int indent = 0;

    public void printProgram(List<Stmt> statements) {
        System.out.println("Program");
        indent = 1;
        for (Stmt stmt : statements) {
            System.out.println(stmt.accept(this));
        }
    }

    private String pad() {
        return "  ".repeat(indent);
    }

    private String child(Object node) {
        indent++;
        String result = (node instanceof Expr e) ? e.accept(this) : ((Stmt) node).accept(this);
        indent--;
        return result;
    }

    // ---- statements ----

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return pad() + "ExpressionStmt\n" + child(stmt.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return pad() + "PrintStmt\n" + child(stmt.expression);
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder sb = new StringBuilder(pad()).append("Block\n");
        indent++;
        for (Stmt s : stmt.statements) sb.append(s.accept(this)).append("\n");
        indent--;
        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder sb = new StringBuilder(pad()).append("If\n");
        indent++;
        sb.append(pad()).append("condition:\n").append(child(stmt.condition)).append("\n");
        sb.append(pad()).append("then:\n").append(child(stmt.thenBranch)).append("\n");
        if (stmt.elseBranch != null) {
            sb.append(pad()).append("else:\n").append(child(stmt.elseBranch)).append("\n");
        }
        indent--;
        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        StringBuilder sb = new StringBuilder(pad()).append("While\n");
        indent++;
        sb.append(pad()).append("condition:\n").append(child(stmt.condition)).append("\n");
        sb.append(pad()).append("body:\n").append(child(stmt.body)).append("\n");
        indent--;
        return sb.substring(0, sb.length() - 1);
    }

    // ---- expressions ----

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return pad() + "Binary (" + expr.operator.lexeme + ")\n"
                + child(expr.left) + "\n" + child(expr.right);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return pad() + "Logical (" + expr.operator.lexeme + ")\n"
                + child(expr.left) + "\n" + child(expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return pad() + "Unary (" + expr.operator.lexeme + ")\n" + child(expr.right);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return pad() + "Literal (" + expr.value + ")";
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return pad() + "Variable (" + expr.name.lexeme + ")";
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return pad() + "Assign (" + expr.name.lexeme + ")\n" + child(expr.value);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return pad() + "Grouping\n" + child(expr.expression);
    }
}
