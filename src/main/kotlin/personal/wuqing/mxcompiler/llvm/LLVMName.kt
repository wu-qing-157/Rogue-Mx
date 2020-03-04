package personal.wuqing.mxcompiler.llvm

sealed class LLVMName {
    class Local(val name: String) : LLVMName() {
        override fun toString() = "%$name"
    }

    class Global(private val name: String) : LLVMName() {
        override fun toString() = "@$name"
    }

    class Const(private val value: Int) : LLVMName() {
        override fun toString() = "$value"
    }

    class Literal(l: Int, s: String) : LLVMName() {
        private val display = l.let {
            ("\\${(it and 255).toString(16).padStart(2, '0')}" +
                    "\\${(it shr 8 and 255).toString(16).padStart(2, '0')}" +
                    "\\${(it shr 16 and 255).toString(16).padStart(2, '0')}" +
                    "\\${(it shr 24 and 255).toString(16).padStart(2, '0')}").toUpperCase()
        } + s.replace(Regex("[^0-9a-zA-Z]")) {
            it.value.toByteArray().joinToString { b -> "\\${b.toString(16).padStart(2, '0').toUpperCase()}" }
        }

        override fun toString() = "c\"$display\""
    }

    object Void : LLVMName() {
        override fun toString() = "@this.name.should.not.appear.in.llvm"
    }
}
