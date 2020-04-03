package personal.wuqing.rogue.ir.grammar

class IRProgram(
    val struct: Iterable<IRType.Class>,
    val global: MutableSet<IRItem>, val literal: MutableSet<IRItem.Literal>,
    val function: MutableSet<IRFunction.Declared>
)
