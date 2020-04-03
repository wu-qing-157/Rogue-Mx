package personal.wuqing.rogue.ir.grammar

enum class IRCalcOp(private val text: String) {
    ADD("add"), SUB("sub"), MUL("mul"), SDIV("sdiv"), SREM("srem"),
    AND("and"), OR("or"), XOR("xor"), SHL("shl"), ASHR("ashr"), LSHR("lshr");

    override fun toString() = text
}
