package kestrel;

public class Symbol {
    public enum Type { NUMBER, STRING, BOOLEAN, FUNCTION, UNKNOWN }

    public final String name;
    public Type type;
    public final int arity; // parameter count, only meaningful for FUNCTION symbols (-1 otherwise)

    public Symbol(String name, Type type) { this(name, type, -1); }
    public Symbol(String name, Type type, int arity) {
        this.name = name; this.type = type; this.arity = arity;
    }

    @Override
    public String toString() {
        return type == Type.FUNCTION
                ? String.format("%-12s FUNCTION(arity=%d)", name, arity)
                : String.format("%-12s %s", name, type);
    }
}
