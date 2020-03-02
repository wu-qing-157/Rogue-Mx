package personal.wuqing.mxcompiler.llvm

enum class ICalcOperator(private val text: String) {
    ADD("add"), SUB("sub");

    override fun toString() = text
}

enum class IComOperator(private val text: String) {
    NE("ne");

    override fun toString() = text
}
