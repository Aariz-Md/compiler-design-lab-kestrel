package kestrel;

public class Instruction {
    public final OpCode op;
    public final Object operand;    // constant value, name, slot index, or jump address
    public final Integer operand2;  // only used by CALL, to carry the argument count

    public Instruction(OpCode op) { this(op, null, null); }
    public Instruction(OpCode op, Object operand) { this(op, operand, null); }
    public Instruction(OpCode op, Object operand, Integer operand2) {
        this.op = op; this.operand = operand; this.operand2 = operand2;
    }

    public String render() {
        if (operand2 != null) return String.format("%-14s %s, %d", op, operand, operand2);
        if (operand instanceof String s) return String.format("%-14s \"%s\"", op, s);
        if (operand != null) return String.format("%-14s %s", op, operand);
        return op.toString();
    }
}
