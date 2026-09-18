package kestrel;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for Kestrel.
 *
 * Grammar implemented (EBNF):
 *
 *   program     -> statement* EOF
 *   statement   -> exprStmt | printStmt | ifStmt | whileStmt | block
 *   exprStmt    -> expression ";"
 *   printStmt   -> "print" "(" expression ")" ";"
 *   ifStmt      -> "if" "(" expression ")" block ( "else" block )?
 *   whileStmt   -> "while" "(" expression ")" block
 *   block       -> "{" statement* "}"
 *
 *   expression  -> assignment
 *   assignment  -> IDENTIFIER "=" assignment | logicOr
 *   logicOr     -> logicAnd ( "||" logicAnd )*
 *   logicAnd    -> equality ( "&&" equality )*
 *   equality    -> comparison ( ("==" | "!=") comparison )*
 *   comparison  -> term ( ("<" | "<=" | ">" | ">=") term )*
 *   term        -> factor ( ("+" | "-") factor )*
 *   factor      -> unary ( ("*" | "/") unary )*
 *   unary       -> ("-" | "!") unary | primary
 *   primary     -> NUMBER | STRING | "true" | "false"
 *                | IDENTIFIER | "(" expression ")"
 */
public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parseProgram() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }
        return statements;
    }

    // ---- statements ----

    private Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.LBRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt printStatement() {
        consume(TokenType.LPAREN, "Expected '(' after 'print'");
        Expr value = expression();
        consume(TokenType.RPAREN, "Expected ')' after print expression");
        consume(TokenType.SEMICOLON, "Expected ';' after print statement");
        return new Stmt.Print(value);
    }

    private Stmt ifStatement() {
        consume(TokenType.LPAREN, "Expected '(' after 'if'");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expected ')' after if condition");

        consume(TokenType.LBRACE, "Expected '{' to start if-branch block");
        Stmt thenBranch = new Stmt.Block(block());

        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            consume(TokenType.LBRACE, "Expected '{' to start else-branch block");
            elseBranch = new Stmt.Block(block());
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(TokenType.LPAREN, "Expected '(' after 'while'");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expected ')' after while condition");
        consume(TokenType.LBRACE, "Expected '{' to start while body");
        Stmt body = new Stmt.Block(block());
        return new Stmt.While(condition, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statements.add(statement());
        }
        consume(TokenType.RBRACE, "Expected '}' after block");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression");
        return new Stmt.Expression(expr);
    }

    // ---- expressions (lowest to highest precedence) ----

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logicOr();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment(); // right-associative
            if (expr instanceof Expr.Variable v) {
                return new Expr.Assign(v.name, value);
            }
            throw new CompilerError("Syntax", equals.line, "Invalid assignment target");
        }
        return expr;
    }

    private Expr logicOr() {
        Expr expr = logicAnd();
        while (match(TokenType.OR_OR)) {
            Token operator = previous();
            Expr right = logicAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr logicAnd() {
        Expr expr = equality();
        while (match(TokenType.AND_AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.LESS, TokenType.LESS_EQUAL,
                     TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.MINUS, TokenType.BANG)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.TRUE)) return new Expr.Literal(Boolean.TRUE);
        if (match(TokenType.FALSE)) return new Expr.Literal(Boolean.FALSE);
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expected ')' after expression");
            return new Expr.Grouping(expr);
        }
        throw new CompilerError("Syntax", peek().line,
                "Unexpected token '" + peek().lexeme + "'");
    }

    // ---- low-level parsing helpers ----

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new CompilerError("Syntax", peek().line,
                message + " (found '" + peek().lexeme + "')");
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
