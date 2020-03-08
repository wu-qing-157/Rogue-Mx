package personal.wuqing.mxcompiler.llvm.grammar

import personal.wuqing.mxcompiler.llvm.map.TypeMap

sealed class LLVMType(val size: Int) {

    object I32 : LLVMType(4) {
        override fun toString() = "i32"
    }

    object I8 : LLVMType(1) {
        override fun toString() = "i8"
    }

    object I1 : LLVMType(1) {
        override fun toString() = "i1"
    }

    class Class(val name: String) : LLVMType(4) {
        override fun toString() = "%__class__.$name"
        override fun equals(other: Any?) = other is Class && name == other.name
        override fun hashCode() = name.hashCode()
        lateinit var members: MemberArrangement private set
        fun init(members: MemberArrangement) {
            this.members = members
        }

        fun definition() = "${toString()} = type { ${members.members.joinToString { TypeMap[it.type].toString() }} }"
    }

    class Pointer(val type: LLVMType) : LLVMType(4) {
        override fun toString() = "$type*"
    }

    companion object {
        val string =
            Pointer(I8)
    }

    object Void : LLVMType(1) {
        override fun toString() = "void"
    }

    class Vector(val length: Int, val base: LLVMType) : LLVMType(length * base.size) {
        override fun toString() = "[ $length x $base ]"
    }

    object Null : LLVMType(4) {
        override fun toString() = "i8*"
    }
}
