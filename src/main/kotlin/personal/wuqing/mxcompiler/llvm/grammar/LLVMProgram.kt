package personal.wuqing.mxcompiler.llvm.grammar

class LLVMProgram(
    val struct: List<LLVMType.Class>, val global: List<LLVMGlobal>,
    val function: List<LLVMFunction.Declared>, val external: List<LLVMFunction.External>
)
