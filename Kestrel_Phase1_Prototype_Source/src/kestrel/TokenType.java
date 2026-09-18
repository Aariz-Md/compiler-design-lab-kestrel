package kestrel;

/**
 * All lexical categories recognised by the Kestrel Lexer.
 * Grouped by kind for readability.
 */
public enum TokenType {
    // Literals
    NUMBER, STRING, IDENTIFIER,

    // Single-character punctuation
    LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON,

    // Operators
    PLUS, MINUS, STAR, SLASH,
    EQUAL,            // assignment  =
    EQUAL_EQUAL,      // ==
    BANG, BANG_EQUAL, // !  !=
    LESS, LESS_EQUAL,
    GREATER, GREATER_EQUAL,
    AND_AND, OR_OR,   // &&  ||

    // Keywords
    IF, ELSE, WHILE, PRINT, TRUE, FALSE,

    EOF
}
