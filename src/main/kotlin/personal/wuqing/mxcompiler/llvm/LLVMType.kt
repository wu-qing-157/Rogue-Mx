package personal.wuqing.mxcompiler.llvm

sealed class LLVMType {
    data class I(val length: Int) : LLVMType() {
        override fun toString() = "i$length"
    }

    class Class(val name: kotlin.String) : LLVMType() {
        override fun toString() = "class.$name"
        override fun equals(other: Any?) = other is Class && name == other.name
        override fun hashCode() = name.hashCode()
        lateinit var members: MemberArrangement private set
        fun init(members: MemberArrangement) {
            this.members = members
        }
        fun definition() = "$name = type { ${members.members.joinToString { Translator[it.type].toString() }} }"
    }

    data class Pointer(val type: LLVMType) : LLVMType() {
        override fun toString() = "$type*"
    }

    object String : LLVMType() {
        override fun toString() = "%string"
    }

    object Void : LLVMType() {
        override fun toString() = "void"
    }
}
