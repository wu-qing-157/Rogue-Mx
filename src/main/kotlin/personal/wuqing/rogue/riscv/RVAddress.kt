package personal.wuqing.rogue.riscv

open class RVAddress(val base: RVRegister, open val delta: Int = 0) {
    override fun toString() = "$delta($base)"

    class Saver(val function: RVFunction, val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = function.size - (index + 1) * 4
    }

    class Stack(val function: RVFunction, val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = function.stackSize + function.passSize - (index + 1) * 4
    }

    class Pass(val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = (index - 8) * 4
    }

    class Caller(val function: RVFunction, val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = function.size + (index - 8) * 4
    }
}
