# Kestrel — Phase 1 Prototype

Lexer + Parser prototype for the Kestrel compiler project (Compiler Design Lab, Phase 1).

## Build

```
mkdir -p bin
javac -d bin src/kestrel/*.java
```

## Run

```
java -cp bin kestrel.Main samples/sample1_arithmetic.kes
java -cp bin kestrel.Main samples/sample2_conditional.kes
java -cp bin kestrel.Main samples/sample3_loop.kes
java -cp bin kestrel.Main samples/sample4_syntax_error.kes   # demonstrates syntax error reporting
```

## What's implemented (Phase 1 scope)

- Lexical Analysis (`Lexer.java`) — tokenizes Kestrel source, detects illegal characters and
  unterminated strings.
- Syntax Analysis (`Parser.java`) — recursive-descent parser building an AST (`Expr.java`,
  `Stmt.java`), reports syntax errors with line numbers.
- `AstPrinter.java` — renders the AST as an indented parse tree for inspection.
- `CompilerError.java` — shared error type (stage, line, message) used across all future
  compilation stages.

## What's planned for Phase 2 / 3

Semantic analysis + symbol table, bytecode generation (intermediate code), bytecode
optimization (constant folding / dead-code elimination), and the stack-based virtual
machine (target code generation + execution), including function calls and recursion.

See `Kestrel_Phase1_Report.docx` for the full design document.
