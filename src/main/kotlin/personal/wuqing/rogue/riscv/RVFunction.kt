package personal.wuqing.rogue.riscv

import kotlin.math.max

class RVFunction(val name: String) {
    override fun toString() = name

    val saver = mutableListOf<Pair<RVRegister, RVAddress>>()
    val metaSize get() = (saver.size / 4 + 1) * 16
    fun nextSaver(register: RVRegister) = RVAddress.Saver(this, saver.size).also {
        saver += register to it
    }

    val stack = mutableListOf<RVAddress.Stack>()
    val stackSize get() = (stack.size + 3) / 4 * 16
    fun nextStack() = RVAddress.Stack(this, stack.size).also { stack += it }

    val passPool = mutableMapOf<Int, RVAddress.Pass>()
    var passMax = 7
    val passSize get() = (passMax - 4) / 4 * 16
    fun getPass(index: Int) =
        passPool.computeIfAbsent(index) { RVAddress.Pass(index) }.also { passMax = max(passMax, index) }

    val size get() = metaSize + stackSize + passSize

    val body = mutableListOf<RVInstruction>()
    val instructions get() = mutableListOf<RVInstruction>().apply {
        add(RVInstruction.Label(name))
        add(RVInstruction.CalcI(RVCalcOp.PLUS, RVRegister.SP, -this@RVFunction.size, RVRegister.SP))
        addAll(saver.map { (reg, saver) -> RVInstruction.Save(reg, saver) })
        addAll(body)
        add(RVInstruction.Label("$name..ret"))
        addAll(saver.map { (reg, saver) -> RVInstruction.Load(reg, saver) })
        add(RVInstruction.CalcI(RVCalcOp.PLUS, RVRegister.SP, this@RVFunction.size, RVRegister.SP))
        add(RVInstruction.JR(RVRegister.RA))
    }
}
