package kestrel;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public final String name;
    public final List<Instruction> instructions = new ArrayList<>();

    public Chunk(String name) { this.name = name; }

    public int emit(Instruction instr) {
        instructions.add(instr);
        return instructions.size() - 1;
    }

    /** Back-patches a previously-emitted JUMP / JUMP_IF_FALSE placeholder to point at the current end of the chunk. */
    public void patchJumpToHere(int index) {
        Instruction placeholder = instructions.get(index);
        instructions.set(index, new Instruction(placeholder.op, instructions.size()));
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chunk '").append(name).append("':\n");
        for (int i = 0; i < instructions.size(); i++) {
            sb.append(String.format("  %4d  %s%n", i, instructions.get(i).render()));
        }
        return sb.toString().stripTrailing();
    }
}
