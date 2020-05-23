package personal.wuqing.rogue.riscv.grammar

import personal.wuqing.rogue.utils.DirectionalNodeWithPrev

class RVBlock(val name: String): DirectionalNodeWithPrev<RVBlock> {
    val instructions = mutableListOf<RVInstruction>()
    override val prev = mutableListOf<RVBlock>()
    override val next = mutableListOf<RVBlock>()
    override fun toString() = name
}
