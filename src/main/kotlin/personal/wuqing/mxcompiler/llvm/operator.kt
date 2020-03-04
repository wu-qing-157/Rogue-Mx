package personal.wuqing.mxcompiler.llvm

enum class ICalcOperator(private val text: String) {
    ADD("add"), SUB("sub"), MUL("mul"), SDIV("sdiv"), SREM("srem"),
    AND("and"), OR("or"), XOR("xor"), SHL("shl"), ASHR("ashr"), LSHR("lshr");

    override fun toString() = text
}

enum class IComOperator(private val text: String) {
    EQ("eq"), NE("ne"), SLT("slt"), SLE("sle"), SGT("sgt"), SGE("sge");

    override fun toString() = text
}
