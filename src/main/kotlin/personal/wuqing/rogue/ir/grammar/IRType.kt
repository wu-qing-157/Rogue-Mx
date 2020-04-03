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
    }

    object I1 : IRType(1) {
        infix fun const(value: Int) = IRItem.Const(this, value)
        override val default = I1 const 0
    }

    object String : IRType(ptr) {
        override val default = IRItem.Null(String)
    }

    data class Array(val base: IRType) : IRType(ptr) {
        override val default = IRItem.Null(this)
    }

    data class Class(private val mx: MxType.Class, val name: kotlin.String) : IRType(ptr) {
        val members by lazy(LazyThreadSafetyMode.NONE) { MemberArrangement(mx) }
        override val default get() = IRItem.Null(this)
    }

    data class Pointer(val base: IRType) : IRType(ptr) {
        override val default get() = IRItem.Null(this)
    }

    object Void : IRType(1) {
        override val default get() = error("getting default value of void")
    }

    object Null : IRType(4) {
        override val default get() = error("getting default value of null")
    }
}
