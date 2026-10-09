package kestrel;

import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    public enum Kind { GLOBAL, FUNCTION, BLOCK }

    public final Kind kind;
    public final Scope enclosing;
    public final String label; // e.g. function name, for readable dumps
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    public Scope(Kind kind, Scope enclosing, String label) {
        this.kind = kind; this.enclosing = enclosing; this.label = label;
    }

    public void define(Symbol symbol) { symbols.put(symbol.name, symbol); }

    public boolean definedHere(String name) { return symbols.containsKey(name); }

    public Symbol getHere(String name) { return symbols.get(name); }

    public Map<String, Symbol> all() { return symbols; }
}
