package personal.wuqing.rogue.riscv.grammar

import personal.wuqing.rogue.utils.DirectionalNodeWithPrev

class RVBlock(
    val name: String, val instructions: MutableList<RVInstruction> = mutableListOf()
) : DirectionalNodeWithPrev<RVBlock> {
    override val prev = mutableListOf<RVBlock>()
    override val next = mutableListOf<RVBlock>()
    override fun toString() = name
}
