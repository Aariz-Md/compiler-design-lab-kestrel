package kestrel;

import java.util.List;

public abstract class Expr {
    public interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitLogicalExpr(Logical expr);
        R visitCallExpr(Call expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Binary extends Expr {
        public final Expr left; public final Token operator; public final Expr right;
        public Binary(Expr left, Token operator, Expr right) { this.left = left; this.operator = operator; this.right = right; }
        public <R> R accept(Visitor<R> v) { return v.visitBinaryExpr(this); }
    }

    public static class Grouping extends Expr {
        public final Expr expression;
        public Grouping(Expr expression) { this.expression = expression; }
        public <R> R accept(Visitor<R> v) { return v.visitGroupingExpr(this); }
    }

    public static class Literal extends Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
        public <R> R accept(Visitor<R> v) { return v.visitLiteralExpr(this); }
    }

    public static class Unary extends Expr {
        public final Token operator; public final Expr right;
        public Unary(Token operator, Expr right) { this.operator = operator; this.right = right; }
        public <R> R accept(Visitor<R> v) { return v.visitUnaryExpr(this); }
    }

    public static class Variable extends Expr {
        public final Token name;
        public Variable(Token name) { this.name = name; }
        public <R> R accept(Visitor<R> v) { return v.visitVariableExpr(this); }
    }

    public static class Assign extends Expr {
        public final Token name; public final Expr value;
        public Assign(Token name, Expr value) { this.name = name; this.value = value; }
        public <R> R accept(Visitor<R> v) { return v.visitAssignExpr(this); }
    }

    public static class Logical extends Expr {
        public final Expr left; public final Token operator; public final Expr right;
        public Logical(Expr left, Token operator, Expr right) { this.left = left; this.operator = operator; this.right = right; }
        public <R> R accept(Visitor<R> v) { return v.visitLogicalExpr(this); }
    }

    // New in Phase 2: function call expression, e.g. fact(n - 1)
    public static class Call extends Expr {
        public final Expr callee; public final Token paren; public final List<Expr> arguments;
        public Call(Expr callee, Token paren, List<Expr> arguments) { this.callee = callee; this.paren = paren; this.arguments = arguments; }
        public <R> R accept(Visitor<R> v) { return v.visitCallExpr(this); }
    }
}
