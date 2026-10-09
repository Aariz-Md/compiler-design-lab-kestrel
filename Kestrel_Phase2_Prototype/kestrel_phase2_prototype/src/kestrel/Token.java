package kestrel;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        String lit = literal != null ? " (" + literal + ")" : "";
        return String.format("%-14s '%s'%s  [line %d]", type, lexeme, lit, line);
    }
}
