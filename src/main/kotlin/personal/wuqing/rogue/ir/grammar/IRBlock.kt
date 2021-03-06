package personal.wuqing.rogue.ir.grammar

import personal.wuqing.rogue.utils.DirectionalNodeWithPrev

class IRBlock constructor(val name: String) : DirectionalNodeWithPrev<IRBlock> {
    val phi = mutableListOf<IRStatement.Phi>()
    val normal = mutableListOf<IRStatement.Normal>()
    lateinit var terminate: IRStatement.Terminate

    override val prev = mutableSetOf<IRBlock>()
    override val next
        get() = when (terminate) {
            is IRStatement.Terminate.Ret -> listOf()
            is IRStatement.Terminate.Jump -> listOf((terminate as IRStatement.Terminate.Jump).dest)
            is IRStatement.Terminate.Branch ->
                (terminate as IRStatement.Terminate.Branch).let { listOf(it.then, it.els) }
        }

    override fun toString() = name
}
