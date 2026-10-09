package kestrel;

public class CompilerError {
    public enum Stage { LEXICAL, SYNTAX, SEMANTIC, RUNTIME }

    public final Stage stage;
    public final int line;
    public final String message;

    public CompilerError(Stage stage, int line, String message) {
        this.stage = stage;
        this.line = line;
        this.message = message;
    }

    @Override
    public String toString() {
        String label = switch (stage) {
            case LEXICAL -> "Lexical Error";
            case SYNTAX -> "Syntax Error";
            case SEMANTIC -> "Semantic Error";
            case RUNTIME -> "Runtime Error";
        };
        return String.format("[line %d] %s: %s", line, label, message);
    }
}
