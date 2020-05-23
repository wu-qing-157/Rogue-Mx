package personal.wuqing.rogue.riscv.grammar

enum class RVCmpOp(val branch: String, val zero: String?) {
    LT("blt", "sltz"), LE("ble", null), GT("bgt", "sgtz"), GE("bge", null), EQ("beq", "seqz"), NE("bne", "snez")
}
