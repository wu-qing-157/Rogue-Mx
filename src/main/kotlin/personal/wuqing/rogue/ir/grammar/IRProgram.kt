package personal.wuqing.rogue.ir.grammar

class IRProgram(
    val global: MutableSet<IRItem.Global>, val literal: MutableSet<IRItem.Literal>,
    val function: MutableSet<IRFunction.Declared>, val main: IRFunction.Declared
)
