package personal.wuqing.rogue.ir.grammar

enum class IRCalcOp(private val text: String) {
    ADD("+"), SUB("-"), MUL("*"), SDIV("/"), SREM("%"),
    AND("and"), OR("or"), XOR("xor"), SHL("shl"), ASHR("shr"), LSHR("ushr");

    override fun toString() = text
}
