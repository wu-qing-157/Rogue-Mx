package personal.wuqing.mxcompiler.llvm.grammar

class LLVMBlock private constructor(val name: LLVMName.Local) {
    val statements = mutableListOf<LLVMStatement>()
    constructor(name: String) : this(LLVMName.Local(name))
}
