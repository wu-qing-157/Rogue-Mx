package personal.wuqing.rogue.ir.grammar

enum class IRCmpOp(private val text: String) {
    EQ("=="), NE("!="), SLT("<"), SLE("<="), SGT(">"), SGE(">=");

    override fun toString() = text
}
