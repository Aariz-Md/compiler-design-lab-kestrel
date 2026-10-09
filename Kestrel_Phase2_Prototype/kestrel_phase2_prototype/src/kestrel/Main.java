package kestrel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("usage: java kestrel.Main <source-file.kes>");
            return;
        }
        String source = Files.readString(Path.of(args[0]));
        String fileLabel = Path.of(args[0]).getFileName().toString();

        System.out.println("=".repeat(78));
        System.out.println("KESTREL PIPELINE  --  " + fileLabel);
        System.out.println("=".repeat(78));

        System.out.println("\nSOURCE:");
        System.out.println("-".repeat(78));
        System.out.print(source.endsWith("\n") ? source : source + "\n");

        // ---- Stage 1: Lexical Analysis ----
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        if (!lexer.errors.isEmpty()) {
            printErrors("LEXICAL ERRORS", lexer.errors);
            return;
        }

        // ---- Stage 2: Syntax Analysis ----
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if (!parser.errors.isEmpty()) {
            printErrors("SYNTAX ERRORS", parser.errors);
            return;
        }

        System.out.println("\nPARSE TREE:");
        System.out.println("-".repeat(78));
        System.out.println(new AstPrinter().print(statements));

        // ---- Stage 3: Semantic Analysis ----
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(statements);

        System.out.println("\nSYMBOL TABLE:");
        System.out.println("-".repeat(78));
        System.out.println(analyzer.table.dump());

        if (!analyzer.errors.isEmpty()) {
            printErrors("SEMANTIC ERRORS", analyzer.errors);
            System.out.println("\n(Bytecode generation skipped: fix the semantic errors above first.)");
            return;
        }

        // ---- Stage 4: Bytecode Generation (IR) ----
        BytecodeGenerator gen = new BytecodeGenerator();
        gen.generate(statements);

        System.out.println("\nBYTECODE (IR):");
        System.out.println("-".repeat(78));
        for (Chunk fn : gen.functionChunks.values()) {
            System.out.println(fn.render());
            System.out.println();
        }
        System.out.println(gen.mainChunk.render());

        System.out.println("\n(No semantic errors. Execution by the Virtual Machine is implemented in Phase 3.)");
    }

    private static void printErrors(String title, List<CompilerError> errors) {
        System.out.println("\n" + title + ":");
        System.out.println("-".repeat(78));
        for (CompilerError e : errors) System.out.println(e);
    }
}
