# Kestrel — Phase 3 Prototype

A hand-written, compiler-course interpreter for a small, statically-scoped,
dynamically-typed language.  Written in **Java 17+**, no external libraries,
no build tool (plain `javac`).

---

## Language Quick Reference

```
// Variables (global by default, local inside functions)
x = 42;
name = "hello";
flag = true;

// Arithmetic & logic
y = (2 + 3) * 4 - 6 / 2;    // 17
b = x > 10 && flag;

// Print
print(y);

// If / else
if (x > 0) { print("pos"); } else { print("neg"); }

// While
while (x > 0) { x = x - 1; }

// Functions (recursive, local scope)
function fact(n) {
    if (n <= 1) { return 1; }
    return n * fact(n - 1);
}
print(fact(10));   // 3628800
```

---

## Build

```
javac -d out src/kestrel/*.java
```

Or use the provided scripts:

| Script            | Platform |
|-------------------|----------|
| `build.bat`       | Windows  |
| `build.sh`        | Unix/Mac |

---

## Run

```
java -cp out kestrel.Main [flags] <file.kes>
```

| Flag       | Effect |
|------------|--------|
| *(none)*   | Full demo: source → parse tree → symbol table → bytecode → optimized bytecode + optimizer report → output → stats |
| `--no-opt` | Skip the optimizer; run the unoptimized bytecode |
| `--quiet`  | Print only the program's own output (runtime errors → stderr) |
| `--trace`  | VM prints each instruction and the stack before executing it |
| `--stats`  | Run both unoptimized and optimized, compare dynamic instruction counts |

**Exit codes:** 0 = success · 65 = compile error · 70 = runtime error

---

## Test Suite

```
java -cp out kestrel.TestRunner     # or: run_tests.bat / run_tests.sh
```

Discovers every `tests/*.kes` and compares output (stdout + stderr) against the
sibling `.expected` file — twice: once **unoptimized** and once **optimized**.
Results are printed to the console and written to `test_results.txt`.

---

## Benchmark

```
java -cp out kestrel.Benchmark      # or: run_benchmark.bat / run_benchmark.sh
```

Runs each program in `samples/bench/` with 3 warm-up + 7 measured runs,
reports static/dynamic instruction counts, median wall time, and speed-up.
Results written to `benchmark_results.txt`.

---

## File Map

### Source — `src/kestrel/`

| File                   | Phase | Role |
|------------------------|-------|------|
| `TokenType.java`       | 1     | Enum of every token kind |
| `Token.java`           | 1     | Token value + source line |
| `Lexer.java`           | 1     | Character → Token stream |
| `Expr.java`            | 1/2   | Expression AST nodes |
| `Stmt.java`            | 1/2   | Statement AST nodes |
| `Parser.java`          | 1/2   | Token stream → AST |
| `AstPrinter.java`      | 1/2   | AST → indented text |
| `Symbol.java`          | 2     | Symbol type/arity record |
| `Scope.java`           | 2     | Single scope in the scope chain |
| `SymbolTable.java`     | 2     | Scope chain + resolution |
| `SemanticAnalyzer.java`| 2     | Type/scope checking |
| `CompilerError.java`   | 1/2   | Compiler error record |
| `OpCode.java`          | 2     | IR instruction set enum |
| `Instruction.java`     | 2/3   | Immutable IR instruction + source line |
| `Chunk.java`           | 2/3   | Named instruction list + arity/localCount |
| `BytecodeGenerator.java`| 2/3  | AST → Chunks with line stamping |
| `Program.java`         | 3     | Compiled program (main + functions) |
| `RuntimeError.java`    | 3     | Runtime exception with line + trace |
| `Ops.java`             | 3     | Shared operator semantics (VM + Optimizer) |
| `VM.java`              | 3     | Fetch-decode-execute VM with explicit call stack |
| `Optimizer.java`       | 3     | Constant folding, branch/peephole/dead-code passes |
| `Pipeline.java`        | 3     | Compile + run pipeline (shared by all tools) |
| `Main.java`            | 3     | CLI entry point |
| `TestRunner.java`      | 3     | Automated test runner |
| `Benchmark.java`       | 3     | Performance benchmark tool |

### Samples — `samples/`

| File | Description |
|------|-------------|
| `sample5_functions_recursion.kes` | Factorial and Fibonacci |
| `sample6_scope_and_shadowing.kes` | Local vs. global scope |
| `sample7_semantic_errors.kes`     | Three deliberate semantic errors |
| `sample8_optimizer_demo.kes`      | Constant folding + dead-branch removal |
| `sample9_runtime_error.kes`       | Division-by-zero with stack trace |
| `sample10_stack_overflow.kes`     | Infinite recursion → stack overflow |
| `bench/bench_fib.kes`             | Recursive fib(27) benchmark |
| `bench/bench_loop.kes`            | 1 000 000-iteration loop benchmark |
| `bench/bench_mixed.kes`           | Nested loops + function calls benchmark |

### Tests — `tests/`

21 tests covering arithmetic, strings, logic, control flow, functions,
scoping, optimizer correctness, and expected errors.

---

## Phase 3 Components

### Virtual Machine (`VM.java`)
- Stack-based, fetch-decode-execute loop
- **Explicit frame stack** — Kestrel recursion does not consume the Java stack
- Globals in `HashMap`, locals in per-frame `Object[]` with `UNSET` sentinel
- MAX_FRAMES = 1 000 (stack overflow protection)
- Post-run statistics: instructions executed, calls, max stack/call depth

### Optimizer (`Optimizer.java`)
Runs until fixed point (max 10 iterations per chunk):
1. **Constant folding** — `CONST a; CONST b; <op>` → `CONST result`
2. **Branch folding** — `CONST true/false; JUMP_IF_FALSE` → eliminate/JUMP
3. **Peephole** — remove STORE/LOAD/POP redundancy, CONST/POP, trivial jumps, thread jumps
4. **Dead-code elimination** — reachability analysis, removes unreachable instructions

Safety: jump targets are remapped after every pass that changes list lengths.
Division-by-zero and type errors are **not** folded (left for runtime with correct line).

### Runtime Error Reporting
```
[line 2] Runtime Error: division by zero
  at divide()
  at main
```
