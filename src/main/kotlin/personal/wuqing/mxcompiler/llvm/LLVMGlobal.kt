package personal.wuqing.mxcompiler.llvm

class LLVMGlobal private constructor(val name: LLVMName, val type: LLVMType, val value: LLVMName) {
    constructor(name: String, type: LLVMType, value: LLVMName) :
            this(LLVMName.Global(name), type, value)

    override fun toString() = "$name = private global $type $value"
}
