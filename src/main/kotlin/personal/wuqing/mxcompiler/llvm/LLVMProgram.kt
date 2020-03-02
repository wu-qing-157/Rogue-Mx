package personal.wuqing.mxcompiler.llvm

class LLVMProgram(
    val struct: List<LLVMType.Class>, val global: List<LLVMGlobal>, val function: List<LLVMFunction.Declared>
)
