package kestrel;

import java.util.List;

/** Abstract syntax tree nodes for statements. */
public abstract class Stmt {

    public interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitBlockStmt(Block stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    /** A bare expression statement, e.g. "x = 5;" */
    public static class Expression extends Stmt {
        public final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }
    }

    /** print(expr); */
    public static class Print extends Stmt {
        public final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitPrintStmt(this); }
    }

    /** { statement* } */
    public static class Block extends Stmt {
        public final List<Stmt> statements;

        public Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }
    }

    /** if (condition) thenBranch [else elseBranch] */
    public static class If extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch; // nullable

        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitIfStmt(this); }
    }

    /** while (condition) body */
    public static class While extends Stmt {
        public final Expr condition;
        public final Stmt body;

        public While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> visitor) { return visitor.visitWhileStmt(this); }
    }
}
