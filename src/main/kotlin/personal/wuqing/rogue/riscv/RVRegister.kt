package personal.wuqing.rogue.riscv

sealed class RVRegister {
    object ZERO : RVRegister() {
        override fun toString() = "x0"
    }

    object SP : RVRegister() {
        override fun toString() = "sp"
    }

    object RA : RVRegister() {
        override fun toString() = "ra"
    }

    class ARG(private val number: Int) : RVRegister() {
        override fun toString() = "a$number"
    }

    class SAVED(private val number: Int) : RVRegister() {
        override fun toString() = "s$number"
    }

    class TEMP(private val number: Int) : RVRegister() {
        override fun toString() = "t$number"
    }

    companion object {
        val arg = Array(7) { ARG(it) }
        val saved = Array(11) { SAVED(it) }
        val temp = Array(6) { TEMP(it) }
    }
}
