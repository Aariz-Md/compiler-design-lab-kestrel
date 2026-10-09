package kestrel;

import java.util.ArrayList;
import java.util.List;

public class SymbolTable {
    private Scope current;
    // Keep every scope ever created (in creation order) so we can print a full dump at the end.
    private final List<Scope> allScopes = new ArrayList<>();

    public SymbolTable() {
        current = new Scope(Scope.Kind.GLOBAL, null, "global");
        allScopes.add(current);
    }

    public Scope pushScope(Scope.Kind kind, String label) {
        Scope s = new Scope(kind, current, label);
        current = s;
        allScopes.add(s);
        return s;
    }

    public void popScope() {
        if (current.enclosing != null) current = current.enclosing;
    }

    public Scope currentScope() { return current; }

    /** Defines a symbol in the innermost (current) scope, e.g. on first assignment or a parameter binding. */
    public void define(Symbol symbol) { current.define(symbol); }

    /** Resolves an identifier by walking outward from the current scope to the global scope. Returns null if undeclared. */
    public Symbol resolve(String name) {
        Scope scope = current;
        while (scope != null) {
            Symbol s = scope.getHere(name);
            if (s != null) return s;
            scope = scope.enclosing;
        }
        return null;
    }

    /**
     * Resolves an identifier as an *assignment target*: walks outward like resolve(),
     * but stops at a function-scope boundary rather than crossing into the caller's
     * scope. This is what makes a first assignment inside a function create a genuine
     * function-local (shadowing any same-named global) instead of silently mutating
     * the caller's variable, while still letting a nested block mutate a variable
     * already declared earlier in the *same* function.
     */
    public Symbol resolveForAssignment(String name) {
        Scope scope = current;
        while (scope != null) {
            Symbol s = scope.getHere(name);
            if (s != null) return s;
            if (scope.kind == Scope.Kind.FUNCTION) break;
            scope = scope.enclosing;
        }
        return null;
    }

    /** True if the name is already declared in the *current* scope specifically (used to detect shadowing vs. reassignment). */
    public boolean definedInCurrentScope(String name) { return current.definedHere(name); }

    /** Human-readable dump of every scope created during analysis, for demonstration/reporting purposes. */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        for (Scope s : allScopes) {
            String parent = s.enclosing == null ? "-" : s.enclosing.label;
            sb.append(String.format("Scope[%s] label=%s parent=%s%n", s.kind, s.label, parent));
            if (s.all().isEmpty()) {
                sb.append("    (empty)\n");
            } else {
                for (Symbol sym : s.all().values()) sb.append("    ").append(sym).append("\n");
            }
        }
        return sb.toString().stripTrailing();
    }
}
