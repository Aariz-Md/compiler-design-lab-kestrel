package kestrel;

/** Kestrel's intermediate representation: a small, stack-oriented instruction set. */
public enum OpCode {
    CONST,          // operand: literal value        -- push a constant
    LOAD_GLOBAL,    // operand: name                 -- push a global variable's value
    STORE_GLOBAL,   // operand: name                 -- pop and store into a global variable
    LOAD_LOCAL,     // operand: slot index            -- push a local variable's value
    STORE_LOCAL,    // operand: slot index            -- pop and store into a local variable

    ADD, SUB, MUL, DIV,                     // pop 2, push 1 (arithmetic)
    EQ, NEQ, LT, LE, GT, GE,                // pop 2, push 1 (comparison -> boolean)
    NEG, NOT,                               // pop 1, push 1 (unary)

    JUMP,           // operand: target address        -- unconditional jump
    JUMP_IF_FALSE,  // operand: target address        -- pop; jump if falsy

    CALL,           // operand: function name, operand2: arg count
    RETURN,         // pop return value, pop call frame, resume caller

    PRINT,          // pop and print
    POP             // discard top of stack
}
