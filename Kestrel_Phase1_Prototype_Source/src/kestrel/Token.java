package kestrel;

/** A single lexical token produced by the Lexer. */
public class Token {
    public final TokenType type;
    public final String lexeme;   // raw source text, e.g. "123", "x", "while"
    public final Object literal;  // parsed value for NUMBER/STRING tokens, else null
    public final int line;        // 1-based source line, used for error messages

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("%-14s '%s'%s", type, lexeme,
                literal != null ? "  (literal=" + literal + ")" : "");
    }
}
