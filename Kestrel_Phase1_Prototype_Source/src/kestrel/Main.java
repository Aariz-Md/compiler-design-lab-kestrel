package kestrel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Prototype driver for Kestrel.
 *
 * Phase 1 scope: Lexical Analysis + Syntax Analysis only.
 * Usage: java kestrel.Main <path-to-source-file>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java kestrel.Main <source-file>");
            return;
        }

        String source = Files.readString(Path.of(args[0]));

        System.out.println("=========================================");
        System.out.println(" SOURCE: " + args[0]);
        System.out.println("=========================================");
        System.out.println(source);

        // ---- Stage 1: Lexical Analysis ----
        System.out.println("-----------------------------------------");
        System.out.println(" STAGE 1: TOKENS (Lexical Analysis)");
        System.out.println("-----------------------------------------");
        List<Token> tokens;
        try {
            Lexer lexer = new Lexer(source);
            tokens = lexer.scanTokens();
            for (Token t : tokens) {
                if (t.type != TokenType.EOF) System.out.println(t);
            }
        } catch (CompilerError e) {
            System.out.println(e.getMessage());
            return;
        }

        // ---- Stage 2: Syntax Analysis / Parsing ----
        System.out.println("-----------------------------------------");
        System.out.println(" STAGE 2: PARSE TREE (Syntax Analysis)");
        System.out.println("-----------------------------------------");
        try {
            Parser parser = new Parser(tokens);
            List<Stmt> program = parser.parseProgram();
            new AstPrinter().printProgram(program);
        } catch (CompilerError e) {
            System.out.println(e.getMessage());
            return;
        }

        System.out.println("-----------------------------------------");
        System.out.println(" Parsed successfully. " +
                "(Semantic analysis, bytecode generation, optimization, and VM");
        System.out.println(" execution are implemented in Phase 2 / Phase 3.)");
        System.out.println("-----------------------------------------");
    }
}
