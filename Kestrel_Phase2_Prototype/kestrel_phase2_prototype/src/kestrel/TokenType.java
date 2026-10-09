package kestrel;

public enum TokenType {
    // Literals
    NUMBER, STRING, IDENTIFIER,

    // Keywords (Phase 1)
    IF, ELSE, WHILE, PRINT, TRUE, FALSE,
    // Keywords (activated in Phase 2)
    FUNCTION, RETURN,

    // Operators
    PLUS, MINUS, STAR, SLASH,
    EQUAL, EQUAL_EQUAL, BANG_EQUAL, BANG,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
    AND_AND, OR_OR,

    // Punctuation
    LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON, COMMA,

    EOF
}
