package personal.wuqing.rogue.riscv.grammar

import java.util.Objects

open class RVAddress(val base: RVRegister, open val delta: Int = 0) {
    override fun toString() = "$delta($base)"
    override fun hashCode() = Objects.hash(base, delta)
    override fun equals(other: Any?) = other is RVAddress && base == other.base && delta == other.delta

    class Stack(val function: RVFunction, val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = function.size - (index + 1) * 4
    }

    class Pass(val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = (index - 8) * 4
    }

    class Caller(val function: RVFunction, val index: Int) : RVAddress(RVRegister.SP) {
        override val delta get() = function.size + (index - 8) * 4
    }
}
