package personal.wuqing.mxcompiler.llvm

sealed class LLVMType {
    object I32 : LLVMType() {
        override fun toString() = "i32"
    }

    object I8 : LLVMType() {
        override fun toString() = "i8"
    }

    object I1 : LLVMType() {
        override fun toString() = "i1"
    }

    class Class(val name: String) : LLVMType() {
        override fun toString() = "%__class__.$name"
        override fun equals(other: Any?) = other is Class && name == other.name
        override fun hashCode() = name.hashCode()
        lateinit var members: MemberArrangement private set
        fun init(members: MemberArrangement) {
            this.members = members
        }

        fun definition() = "${toString()} = type { ${members.members.joinToString { Translator[it.type].toString() }} }"
    }

    class Pointer(val type: LLVMType) : LLVMType() {
        override fun toString() = "$type*"
    }

    companion object {
        val string = Pointer(I8)
    }

    object Void : LLVMType() {
        override fun toString() = "void"
    }

    class Vector(val length: Int, val base: LLVMType) : LLVMType() {
        override fun toString() = "[ $length x $base ]"
    }

    object Null : LLVMType() {
        override fun toString() = "i8*"
    }
}
