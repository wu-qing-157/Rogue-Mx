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
}
