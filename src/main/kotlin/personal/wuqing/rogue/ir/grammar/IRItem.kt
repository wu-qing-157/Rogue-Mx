package personal.wuqing.rogue.ir.grammar

sealed class IRItem(val type: IRType) {
    class Local(type: IRType) : IRItem(type)
    class Global(type: IRType) : IRItem(type)
    class Const(type: IRType, val value: Int) : IRItem(type)
    class Literal(val value: String) : IRItem(IRType.String) {
        private val llvmFormat = value.replace(Regex("[^0-9a-zA-Z]")) {
            it.value.toByteArray().joinToString("") { b -> "\\%02X".format(b) }
        } + "\\00"
    }

    object Void : IRItem(IRType.Void)
    class Null(type: IRType) : IRItem(type)
}
