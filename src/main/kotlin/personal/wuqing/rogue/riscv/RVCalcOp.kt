package personal.wuqing.rogue.riscv

enum class RVCalcOp(val binary: String, val imm: String?) {
    PLUS("add", "addi"), MINUS("sub", null), TIMES("mul", null), DIV("div", null), REM("rem", null),
    SHL("sll", "slli"), SHR("sra", "srai"), USHR("srl", "srli"),
    LT("slt", "slti"),
    AND("and", "andi"), OR("or", "ori"), XOR("xor", "xori")
}