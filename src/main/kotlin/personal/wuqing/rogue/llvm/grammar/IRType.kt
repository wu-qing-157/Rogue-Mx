package personal.wuqing.rogue.llvm.grammar

import personal.wuqing.rogue.A64
import personal.wuqing.rogue.llvm.map.TypeMap

sealed class IRType(val size: Int) {

    object I32 : IRType(4) {
        infix fun const(value: Int) = IRItem.Const(this, value)
        override fun toString() = "i32"
    }

    object I8 : IRType(1) {
        infix fun const(value: Int) = IRItem.Const(this, value)
        override fun toString() = "i8"
    }

    object I1 : IRType(1) {
        infix fun const(value: Int) = IRItem.Const(this, value)
        override fun toString() = "i1"
    }

    class Class(val name: String) : IRType(4) {
        override fun toString() = "%class.$name"
        override fun equals(other: Any?) = other is Class && name == other.name
        override fun hashCode() = name.hashCode()
        lateinit var members: MemberArrangement private set
        fun init(members: MemberArrangement) {
            this.members = members
        }

        fun definition() = "${toString()} = type { ${members.members.joinToString { TypeMap[it.type].toString() }} }"
    }

    data class Pointer(val base: IRType) : IRType(if (A64) 8 else 4) {
        override fun toString() = "$base*"
    }

    companion object {
        val I8P = Pointer(I8)
        val string = Pointer(I8)
    }

    object Void : IRType(1) {
        override fun toString() = "void"
    }

    class Vector(val length: Int, val base: IRType) : IRType(length * base.size) {
        override fun toString() = "[ $length x $base ]"
    }

    object Null : IRType(4) {
        override fun toString() = "i8*"
    }
}
