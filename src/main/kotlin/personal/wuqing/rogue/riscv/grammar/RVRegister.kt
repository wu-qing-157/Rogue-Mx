package personal.wuqing.rogue.riscv.grammar

sealed class RVRegister {
    object ZERO : RVRegister() {
        override fun toString() = "zero"
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
        val arg = Array(8) { ARG(it) }
        val saved = Array(12) { SAVED(it) }
        val temp = Array(7) { TEMP(it) }
        val all = listOf<RVRegister>() + arg + saved + temp
    }

    class Virtual : RVRegister() {
        companion object {
            private var count = 0
        }

        val name = "?${count++}"
        override fun toString() = name
    }

    class Spilled(val address: RVAddress) : RVRegister() {
        companion object {
            private var count = 0
        }

        val name get() = "!$address"
        override fun toString() = name
    }
}
