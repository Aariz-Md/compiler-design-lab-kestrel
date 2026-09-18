package kestrel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lexical Analyzer for Kestrel.
 *
 * Converts raw source text into a list of Tokens, performing:
 *  - keyword vs identifier classification
 *  - number and string literal recognition
 *  - operator / punctuation recognition (including two-char operators)
 *  - lexical error detection (illegal characters, unterminated strings)
 */
public class Lexer {

    private static final Map<String, TokenType> KEYWORDS = Map.of(
            "if", TokenType.IF,
            "else", TokenType.ELSE,
            "while", TokenType.WHILE,
            "print", TokenType.PRINT,
            "true", TokenType.TRUE,
            "false", TokenType.FALSE
    );

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;   // start of current lexeme
    private int current = 0; // current scan position
    private int line = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> add(TokenType.LPAREN);
            case ')' -> add(TokenType.RPAREN);
            case '{' -> add(TokenType.LBRACE);
            case '}' -> add(TokenType.RBRACE);
            case ';' -> add(TokenType.SEMICOLON);
            case '+' -> add(TokenType.PLUS);
            case '-' -> add(TokenType.MINUS);
            case '*' -> add(TokenType.STAR);
            case '=' -> add(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '!' -> add(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '<' -> add(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> add(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '&' -> {
                if (match('&')) add(TokenType.AND_AND);
                else throw new CompilerError("Lexical", line, "Unexpected character '&'");
            }
            case '|' -> {
                if (match('|')) add(TokenType.OR_OR);
                else throw new CompilerError("Lexical", line, "Unexpected character '|'");
            }
            case '/' -> {
                if (match('/')) {
                    // line comment: consume until end of line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    add(TokenType.SLASH);
                }
            }
            case ' ', '\r', '\t' -> { /* ignore whitespace */ }
            case '\n' -> line++;
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new CompilerError("Lexical", line,
                            "Unexpected character '" + c + "'");
                }
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        add(type);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // consume '.'
            while (isDigit(peek())) advance();
        }
        double value = Double.parseDouble(source.substring(start, current));
        add(TokenType.NUMBER, value);
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            throw new CompilerError("Lexical", line, "Unterminated string literal");
        }
        advance(); // closing quote
        String value = source.substring(start + 1, current - 1);
        add(TokenType.STRING, value);
    }

    // ---- low-level scan helpers ----

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private char peekNext() {
        return (current + 1 >= source.length()) ? '\0' : source.charAt(current + 1);
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void add(TokenType type) {
        add(type, null);
    }

    private void add(TokenType type, Object literal) {
        tokens.add(new Token(type, source.substring(start, current), literal, line));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
