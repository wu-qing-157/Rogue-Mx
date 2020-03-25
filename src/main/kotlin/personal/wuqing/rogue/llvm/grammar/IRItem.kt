package personal.wuqing.rogue.llvm.grammar

sealed class IRItem(val type: IRType) {
    operator fun component1() = type
    operator fun component2() = display

    abstract val display: String
    override fun toString() = display

    class Local(type: IRType, val name: String) : IRItem(type) {
        override val display = "%$name"
    }

    class Global(type: IRType, val name: String) : IRItem(type) {
        override val display = "@$name"
    }

    class Const(type: IRType, val value: Int) : IRItem(type) {
        override val display = "$value"
    }

    class Literal(val value: String) : IRItem(IRType.Vector(value.toByteArray().size + 1, IRType.I8)) {
        private val llvmFormat = value.replace(Regex("[^0-9a-zA-Z]")) {
            it.value.toByteArray().joinToString { b -> "\\%02X".format(b) }
        } + "\\00"
        override val display = "c\"$llvmFormat\""
    }

    object Void : IRItem(IRType.Void) {
        override val display = "void"
    }

    class Null(type: IRType) : IRItem(type) {
        override val display = "null"
    }
}
