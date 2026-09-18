package kestrel;

/**
 * Abstract syntax tree nodes for expressions.
 * Uses the classic Visitor pattern so later phases (semantic analysis,
 * bytecode generation) can add new operations without touching this file.
 */
public abstract class Expr {

    public interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitLogicalExpr(Logical expr);
        R visitUnaryExpr(Unary expr);
        R visitLiteralExpr(Literal expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitGroupingExpr(Grouping expr);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    /** e.g. left + right, left < right */
    public static class Binary extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBinaryExpr(this); }
    }

    /** e.g. left && right, left || right (kept separate from Binary for short-circuit semantics later) */
    public static class Logical extends Expr {
        public final Expr left;
        public final Token operator;
        public final Expr right;

        public Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitLogicalExpr(this); }
    }

    /** e.g. -x, !flag */
    public static class Unary extends Expr {
        public final Token operator;
        public final Expr right;

        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitUnaryExpr(this); }
    }

    /** A raw literal value: number, string, true, false */
    public static class Literal extends Expr {
        public final Object value;

        public Literal(Object value) {
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitLiteralExpr(this); }
    }

    /** A variable reference, e.g. x */
    public static class Variable extends Expr {
        public final Token name;

        public Variable(Token name) {
            this.name = name;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitVariableExpr(this); }
    }

    /** An assignment, e.g. x = 5 */
    public static class Assign extends Expr {
        public final Token name;
        public final Expr value;

        public Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitAssignExpr(this); }
    }

    /** A parenthesised expression, e.g. (x + y) */
    public static class Grouping extends Expr {
        public final Expr expression;

        public Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitGroupingExpr(this); }
    }
}
