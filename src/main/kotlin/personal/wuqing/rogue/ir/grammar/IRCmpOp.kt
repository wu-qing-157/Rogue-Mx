package personal.wuqing.rogue.ir.grammar

enum class IRCmpOp(private val text: String) {
    EQ("eq"), NE("ne"), SLT("slt"), SLE("sle"), SGT("sgt"), SGE("sge");

    override fun toString() = text
}
