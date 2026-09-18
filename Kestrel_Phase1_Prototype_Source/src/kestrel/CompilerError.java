package kestrel;

/**
 * Base class for all compile-time errors (lexical, syntax, semantic).
 * Runtime/VM errors will extend this same hierarchy in Phase 2/3.
 */
public class CompilerError extends RuntimeException {
    public final int line;
    public final String stage; // "Lexical" | "Syntax" | "Semantic" | "Runtime"

    public CompilerError(String stage, int line, String message) {
        super(message);
        this.stage = stage;
        this.line = line;
    }

    @Override
    public String getMessage() {
        return String.format("[line %d] %s Error: %s", line, stage, super.getMessage());
    }
}
