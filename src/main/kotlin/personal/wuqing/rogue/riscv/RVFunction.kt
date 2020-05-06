package personal.wuqing.rogue.riscv

import kotlin.math.max

class RVFunction(val name: String) {
    override fun toString() = name

    val stack = mutableListOf<RVAddress.Stack>()
    val stackSize get() = (stack.size + 3) / 4 * 16
    fun nextStack() = RVAddress.Stack(this, stack.size).also { stack += it }

    val passPool = mutableMapOf<Int, RVAddress.Pass>()
    var passMax = 7
    val passSize get() = (passMax - 4) / 4 * 16
    fun getPass(index: Int) =
        passPool.computeIfAbsent(index) { RVAddress.Pass(index) }.also { passMax = max(passMax, index) }

    val size get() = stackSize + passSize

    val body = mutableListOf<RVBlock>()
}
