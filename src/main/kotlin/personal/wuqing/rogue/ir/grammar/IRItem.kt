package personal.wuqing.rogue.ir.grammar

sealed class IRItem {
    companion object {
        var localCount = 0
        var globalCount = 0
        var literalCount = 0
    }

    class Local : IRItem() {
        val name = "v${localCount++}"
        override fun toString() = name
    }

    class Global : IRItem() {
        val name = "g${globalCount++}"
        override fun toString() = name
    }

    class Const(val value: Int) : IRItem() {
        override fun toString() = "$value"
    }

    class Literal(val value: String) : IRItem() {
        val length = value.length
        val name = "s${literalCount++}"
        override fun toString() = name
        val irDisplay = "\"${value.replace(Regex("[\\n\\r\\t\\e\\\\]")) {
            when (it.value) {
                "\n" -> "\\n"
                "\r" -> "\\r"
                "\t" -> "\\t"
                "\u001b" -> "\\e"
                "\\" -> "\\\\"
                else -> ""
            }
        }}\""
        val asmDisplay = "\"${value.replace(Regex("[^a-zA-Z0-9]")) {
            it.value.toByteArray().joinToString { b -> "\\%03o".format(b) }
        }}\""
    }
}
