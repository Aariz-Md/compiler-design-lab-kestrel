package kestrel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static kestrel.TokenType.*;

/**
 * Lowers the AST into Kestrel's stack-oriented bytecode (the IR). Each function
 * declaration compiles into its own Chunk, with parameters pre-assigned to local
 * slots 0..n-1; top-level code compiles into a single "main" chunk using global
 * variable instructions instead of local-slot ones.
 */
public class BytecodeGenerator implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    public final Chunk mainChunk = new Chunk("main");
    public final Map<String, Chunk> functionChunks = new LinkedHashMap<>();

    private Chunk current = mainChunk;
    private Map<String, Integer> localSlots = null; // null => currently generating global/top-level code
    private int nextSlot = 0;

    public void generate(List<Stmt> statements) {
        for (Stmt s : statements) s.accept(this);
    }

    private int emit(OpCode op) { return current.emit(new Instruction(op)); }
    private int emit(OpCode op, Object operand) { return current.emit(new Instruction(op, operand)); }
    private int emit(OpCode op, Object operand, int operand2) { return current.emit(new Instruction(op, operand, operand2)); }

    // ---------------- statements ----------------

    @Override public Void visitExpressionStmt(Stmt.Expression stmt) {
        stmt.expression.accept(this);
        emit(OpCode.POP); // discard the unused result of a bare expression statement
        return null;
    }

    @Override public Void visitPrintStmt(Stmt.Print stmt) {
        stmt.expression.accept(this);
        emit(OpCode.PRINT);
        return null;
    }

    @Override public Void visitIfStmt(Stmt.If stmt) {
        stmt.condition.accept(this);
        int jFalse = emit(OpCode.JUMP_IF_FALSE, -1);
        stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null) {
            int jEnd = emit(OpCode.JUMP, -1);
            current.patchJumpToHere(jFalse);
            stmt.elseBranch.accept(this);
            current.patchJumpToHere(jEnd);
        } else {
            current.patchJumpToHere(jFalse);
        }
        return null;
    }

    @Override public Void visitWhileStmt(Stmt.While stmt) {
        int loopStart = current.instructions.size();
        stmt.condition.accept(this);
        int jFalse = emit(OpCode.JUMP_IF_FALSE, -1);
        stmt.body.accept(this);
        emit(OpCode.JUMP, loopStart);
        current.patchJumpToHere(jFalse);
        return null;
    }

    @Override public Void visitBlockStmt(Stmt.Block stmt) {
        for (Stmt s : stmt.statements) s.accept(this);
        return null;
    }

    @Override public Void visitFunctionStmt(Stmt.Function stmt) {
        Chunk fnChunk = new Chunk(stmt.name.lexeme);
        Map<String, Integer> slots = new LinkedHashMap<>();
        for (int i = 0; i < stmt.params.size(); i++) slots.put(stmt.params.get(i).lexeme, i);

        Chunk prevChunk = current;
        Map<String, Integer> prevSlots = localSlots;
        int prevNext = nextSlot;

        current = fnChunk;
        localSlots = slots;
        nextSlot = slots.size();

        for (Stmt s : stmt.body) s.accept(this);
        if (fnChunk.instructions.isEmpty() || fnChunk.instructions.get(fnChunk.instructions.size() - 1).op != OpCode.RETURN) {
            emit(OpCode.CONST, 0.0); // implicit "return 0" if control falls off the end
            emit(OpCode.RETURN);
        }
        functionChunks.put(stmt.name.lexeme, fnChunk);

        current = prevChunk;
        localSlots = prevSlots;
        nextSlot = prevNext;
        return null;
    }

    @Override public Void visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value != null) stmt.value.accept(this);
        else emit(OpCode.CONST, 0.0);
        emit(OpCode.RETURN);
        return null;
    }

    // ---------------- expressions ----------------

    @Override public Void visitLiteralExpr(Expr.Literal expr) {
        emit(OpCode.CONST, expr.value);
        return null;
    }

    @Override public Void visitGroupingExpr(Expr.Grouping expr) {
        expr.expression.accept(this);
        return null;
    }

    @Override public Void visitVariableExpr(Expr.Variable expr) {
        Integer slot = localSlots == null ? null : localSlots.get(expr.name.lexeme);
        if (slot != null) emit(OpCode.LOAD_LOCAL, slot);
        else emit(OpCode.LOAD_GLOBAL, expr.name.lexeme);
        return null;
    }

    @Override public Void visitAssignExpr(Expr.Assign expr) {
        expr.value.accept(this);
        if (localSlots != null) {
            Integer slot = localSlots.get(expr.name.lexeme);
            if (slot == null) { slot = nextSlot++; localSlots.put(expr.name.lexeme, slot); }
            emit(OpCode.STORE_LOCAL, slot);
            emit(OpCode.LOAD_LOCAL, slot); // an assignment expression evaluates to the assigned value
        } else {
            emit(OpCode.STORE_GLOBAL, expr.name.lexeme);
            emit(OpCode.LOAD_GLOBAL, expr.name.lexeme);
        }
        return null;
    }

    @Override public Void visitUnaryExpr(Expr.Unary expr) {
        expr.right.accept(this);
        emit(expr.operator.type == MINUS ? OpCode.NEG : OpCode.NOT);
        return null;
    }

    @Override public Void visitLogicalExpr(Expr.Logical expr) {
        // Short-circuit evaluation using only JUMP / JUMP_IF_FALSE / CONST, assuming
        // boolean operands (enforced, with warnings, by the Semantic Analyzer).
        if (expr.operator.type == AND_AND) {
            expr.left.accept(this);
            int jFalse = emit(OpCode.JUMP_IF_FALSE, -1);
            expr.right.accept(this);
            int jEnd = emit(OpCode.JUMP, -1);
            current.patchJumpToHere(jFalse);
            emit(OpCode.CONST, false);
            current.patchJumpToHere(jEnd);
        } else { // OR_OR
            expr.left.accept(this);
            int jFalse = emit(OpCode.JUMP_IF_FALSE, -1);
            emit(OpCode.CONST, true);
            int jEnd = emit(OpCode.JUMP, -1);
            current.patchJumpToHere(jFalse);
            expr.right.accept(this);
            current.patchJumpToHere(jEnd);
        }
        return null;
    }

    @Override public Void visitBinaryExpr(Expr.Binary expr) {
        expr.left.accept(this);
        expr.right.accept(this);
        emit(switch (expr.operator.type) {
            case PLUS -> OpCode.ADD;
            case MINUS -> OpCode.SUB;
            case STAR -> OpCode.MUL;
            case SLASH -> OpCode.DIV;
            case EQUAL_EQUAL -> OpCode.EQ;
            case BANG_EQUAL -> OpCode.NEQ;
            case LESS -> OpCode.LT;
            case LESS_EQUAL -> OpCode.LE;
            case GREATER -> OpCode.GT;
            case GREATER_EQUAL -> OpCode.GE;
            default -> throw new IllegalStateException("unexpected binary operator " + expr.operator.type);
        });
        return null;
    }

    @Override public Void visitCallExpr(Expr.Call expr) {
        for (Expr arg : expr.arguments) arg.accept(this);
        String name = ((Expr.Variable) expr.callee).name.lexeme;
        emit(OpCode.CALL, name, expr.arguments.size());
        return null;
    }
}
