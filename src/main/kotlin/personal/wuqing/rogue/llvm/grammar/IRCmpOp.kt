package personal.wuqing.rogue.llvm.grammar

enum class IRCmpOp(private val text: String) {
    EQ("eq"), NE("ne"), SLT("slt"), SLE("sle"), SGT("sgt"), SGE("sge");

    override fun toString() = text
}
