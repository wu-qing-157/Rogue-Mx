package personal.wuqing.mxcompiler.llvm.grammar

enum class LLVMCmp(private val text: String) {
    EQ("eq"), NE("ne"), SLT("slt"), SLE("sle"), SGT("sgt"), SGE("sge");

    override fun toString() = text
}
