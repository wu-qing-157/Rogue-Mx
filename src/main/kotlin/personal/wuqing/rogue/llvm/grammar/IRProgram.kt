package personal.wuqing.rogue.llvm.grammar

class IRProgram(
    val struct: List<IRType.Class>, val global: List<IRGlobal>,
    val function: List<IRFunction.Declared>, val external: List<IRFunction.External>
)
