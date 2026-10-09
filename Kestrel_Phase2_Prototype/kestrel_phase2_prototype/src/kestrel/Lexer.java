package kestrel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    public final List<CompilerError> errors = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("return", TokenType.RETURN);
    }

    public Lexer(String source) { this.source = source; }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() { return current >= source.length(); }
    private char advance() { return source.charAt(current++); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
    private char peekNext() { return current + 1 >= source.length() ? '\0' : source.charAt(current + 1); }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private void addToken(TokenType type) { addToken(type, null); }
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LPAREN);
            case ')' -> addToken(TokenType.RPAREN);
            case '{' -> addToken(TokenType.LBRACE);
            case '}' -> addToken(TokenType.RBRACE);
            case ';' -> addToken(TokenType.SEMICOLON);
            case ',' -> addToken(TokenType.COMMA);
            case '+' -> addToken(TokenType.PLUS);
            case '-' -> addToken(TokenType.MINUS);
            case '*' -> addToken(TokenType.STAR);
            case '/' -> {
                if (match('/')) { while (peek() != '\n' && !isAtEnd()) advance(); }
                else addToken(TokenType.SLASH);
            }
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '&' -> { if (match('&')) addToken(TokenType.AND_AND); else errors.add(err("unexpected character '&'")); }
            case '|' -> { if (match('|')) addToken(TokenType.OR_OR); else errors.add(err("unexpected character '|'")); }
            case ' ', '\r', '\t' -> {}
            case '\n' -> line++;
            case '"' -> string();
            default -> {
                if (Character.isDigit(c)) number();
                else if (Character.isLetter(c) || c == '_') identifier();
                else errors.add(err("unexpected character '" + c + "'"));
            }
        }
    }

    private CompilerError err(String msg) { return new CompilerError(CompilerError.Stage.LEXICAL, line, msg); }

    private void string() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') { errors.add(new CompilerError(CompilerError.Stage.LEXICAL, startLine, "unterminated string")); return; }
            sb.append(advance());
        }
        if (isAtEnd()) { errors.add(new CompilerError(CompilerError.Stage.LEXICAL, startLine, "unterminated string")); return; }
        advance();
        addToken(TokenType.STRING, sb.toString());
    }

    private void number() {
        while (Character.isDigit(peek())) advance();
        if (peek() == '.' && Character.isDigit(peekNext())) {
            advance();
            while (Character.isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (Character.isLetterOrDigit(peek()) || peek() == '_') advance();
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }
}
