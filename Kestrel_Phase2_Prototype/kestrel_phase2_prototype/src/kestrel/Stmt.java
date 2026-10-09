package kestrel;

import java.util.List;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitIfStmt(If stmt);
        R visitWhileStmt(While stmt);
        R visitBlockStmt(Block stmt);
        R visitFunctionStmt(Function stmt);
        R visitReturnStmt(Return stmt);
    }

    public abstract <R> R accept(Visitor<R> visitor);

    public static class Expression extends Stmt {
        public final Expr expression;
        public Expression(Expr expression) { this.expression = expression; }
        public <R> R accept(Visitor<R> v) { return v.visitExpressionStmt(this); }
    }

    public static class Print extends Stmt {
        public final Expr expression;
        public Print(Expr expression) { this.expression = expression; }
        public <R> R accept(Visitor<R> v) { return v.visitPrintStmt(this); }
    }

    public static class If extends Stmt {
        public final Expr condition; public final Stmt thenBranch; public final Stmt elseBranch;
        public If(Expr condition, Stmt thenBranch, Stmt elseBranch) { this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch; }
        public <R> R accept(Visitor<R> v) { return v.visitIfStmt(this); }
    }

    public static class While extends Stmt {
        public final Expr condition; public final Stmt body;
        public While(Expr condition, Stmt body) { this.condition = condition; this.body = body; }
        public <R> R accept(Visitor<R> v) { return v.visitWhileStmt(this); }
    }

    public static class Block extends Stmt {
        public final List<Stmt> statements;
        public Block(List<Stmt> statements) { this.statements = statements; }
        public <R> R accept(Visitor<R> v) { return v.visitBlockStmt(this); }
    }

    // New in Phase 2: function declaration, e.g. function fact(n) { ... }
    public static class Function extends Stmt {
        public final Token name; public final List<Token> params; public final List<Stmt> body;
        public Function(Token name, List<Token> params, List<Stmt> body) { this.name = name; this.params = params; this.body = body; }
        public <R> R accept(Visitor<R> v) { return v.visitFunctionStmt(this); }
    }

    // New in Phase 2: return statement, e.g. return n * fact(n - 1);
    public static class Return extends Stmt {
        public final Token keyword; public final Expr value;
        public Return(Token keyword, Expr value) { this.keyword = keyword; this.value = value; }
        public <R> R accept(Visitor<R> v) { return v.visitReturnStmt(this); }
    }
}
