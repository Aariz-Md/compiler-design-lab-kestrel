package kestrel;

import java.util.ArrayList;
import java.util.List;

import static kestrel.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    public final List<CompilerError> errors = new ArrayList<>();

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt s = declaration();
            if (s != null) statements.add(s);
        }
        return statements;
    }

    // ---- statement level ----
    private Stmt declaration() {
        try {
            if (match(FUNCTION)) return functionDecl();
            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    private Stmt functionDecl() {
        Token name = consume(IDENTIFIER, "expected function name");
        consume(LPAREN, "expected '(' after function name");
        List<Token> params = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                params.add(consume(IDENTIFIER, "expected parameter name"));
            } while (match(COMMA));
        }
        consume(RPAREN, "expected ')' after parameters");
        consume(LBRACE, "expected '{' before function body");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, body);
    }

    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(LBRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) value = expression();
        consume(SEMICOLON, "expected ';' after return value");
        return new Stmt.Return(keyword, value);
    }

    private Stmt ifStatement() {
        consume(LPAREN, "expected '(' after 'if'");
        Expr condition = expression();
        consume(RPAREN, "expected ')' after if condition");
        consume(LBRACE, "expected '{' to start if-block");
        Stmt thenBranch = new Stmt.Block(block());
        Stmt elseBranch = null;
        if (match(ELSE)) {
            consume(LBRACE, "expected '{' to start else-block");
            elseBranch = new Stmt.Block(block());
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LPAREN, "expected '(' after 'while'");
        Expr condition = expression();
        consume(RPAREN, "expected ')' after while condition");
        consume(LBRACE, "expected '{' to start while-block");
        Stmt body = new Stmt.Block(block());
        return new Stmt.While(condition, body);
    }

    private Stmt printStatement() {
        consume(LPAREN, "expected '(' after 'print'");
        Expr value = expression();
        consume(RPAREN, "expected ')' after print expression");
        consume(SEMICOLON, "expected ';' after print statement");
        return new Stmt.Print(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            Stmt s = declaration();
            if (s != null) statements.add(s);
        }
        consume(RBRACE, "expected '}' to close block");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "expected ';' after expression");
        return new Stmt.Expression(expr);
    }

    // ---- expression level: precedence climbing ----
    private Expr expression() { return assignment(); }

    private Expr assignment() {
        Expr expr = logicOr();
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable v) return new Expr.Assign(v.name, value);
            errors.add(err(equals, "invalid assignment target"));
        }
        return expr;
    }

    private Expr logicOr() {
        Expr expr = logicAnd();
        while (match(OR_OR)) { Token op = previous(); expr = new Expr.Logical(expr, op, logicAnd()); }
        return expr;
    }

    private Expr logicAnd() {
        Expr expr = equality();
        while (match(AND_AND)) { Token op = previous(); expr = new Expr.Logical(expr, op, equality()); }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(EQUAL_EQUAL, BANG_EQUAL)) { Token op = previous(); expr = new Expr.Binary(expr, op, comparison()); }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) { Token op = previous(); expr = new Expr.Binary(expr, op, term()); }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(PLUS, MINUS)) { Token op = previous(); expr = new Expr.Binary(expr, op, factor()); }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(STAR, SLASH)) { Token op = previous(); expr = new Expr.Binary(expr, op, unary()); }
        return expr;
    }

    private Expr unary() {
        if (match(MINUS, BANG)) { Token op = previous(); return new Expr.Unary(op, unary()); }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (match(LPAREN)) expr = finishCall(expr);
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> args = new ArrayList<>();
        if (!check(RPAREN)) {
            do { args.add(expression()); } while (match(COMMA));
        }
        Token paren = consume(RPAREN, "expected ')' after arguments");
        return new Expr.Call(callee, paren, args);
    }

    private Expr primary() {
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(LPAREN)) {
            Expr expr = expression();
            consume(RPAREN, "expected ')' after expression");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "expected an expression");
    }

    // ---- token helpers ----
    private boolean match(TokenType... types) {
        for (TokenType t : types) if (check(t)) { advance(); return true; }
        return false;
    }
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }
    private boolean check(TokenType type) { return !isAtEnd() && peek().type == type; }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type == EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }

    private ParseError error(Token token, String message) {
        errors.add(err(token, message));
        return new ParseError();
    }
    private CompilerError err(Token token, String message) {
        String where = token.type == EOF ? "end of input" : "'" + token.lexeme + "'";
        return new CompilerError(CompilerError.Stage.SYNTAX, token.line, message + " (found " + where + ")");
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case IF, WHILE, PRINT, FUNCTION, RETURN -> { return; }
                default -> {}
            }
            advance();
        }
    }
}
