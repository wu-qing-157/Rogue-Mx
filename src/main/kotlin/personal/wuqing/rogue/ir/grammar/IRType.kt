package personal.wuqing.rogue.ir.grammar

import personal.wuqing.rogue.A64
import personal.wuqing.rogue.grammar.MxType

sealed class IRType(val size: Int) {
    abstract val default: IRItem

    companion object {
        val ptr get() = if (A64) 8 else 4
    }

    object I32 : IRType(4) {
        infix fun const(value: Int) = IRItem.Const(this, value)
        override val default = I32 const 0
        override fun toString() = "i32"
    }

    object I1 : IRType(1) {
        infix fun const(value: Int) = IRItem.Const(this, value)
        override val default = I1 const 0
        override fun toString() = "i1"
    }

    object String : IRType(ptr) {
        override val default = IRItem.Null(String)
        override fun toString() = "{i32, i8*}*"
    }

    data class Array(val base: IRType) : IRType(ptr) {
        override val default = IRItem.Null(this)
        override fun toString() = "{i32, $base*}*"
        val reference = "{i32, $base*}"
    }

    data class Class(private val mx: MxType.Class, val name: kotlin.String) : IRType(ptr) {
        val members by lazy(LazyThreadSafetyMode.NONE) { MemberArrangement(mx) }
        override val default get() = IRItem.Null(this)
        override fun toString() = "$reference*"
        val reference = members.types.joinToString(prefix = "{", postfix = "}")
    }

    data class Address(val type: IRType) : IRType(ptr) {
        override val default get() = error("getting default value of address")
        override fun toString() = "$type*"
    }

    object Null : IRType(ptr) {
        override val default get() = error("getting default value of null")
        override fun toString() = "null"
    }

    object Void : IRType(0) {
        override val default get() = error("getting default value of void")
        override fun toString() = "void"
    }
}
