package personal.wuqing.mxcompiler.grammar.operator

import personal.wuqing.mxcompiler.grammar.MxType

enum class MxSuffix(
    private val displayText: kotlin.String, private val requireLvalue: Boolean,
    private val ret: Map<MxType.Primitive, MxType.Primitive>
) {
    INC("++", true, mapOf(Int to Int)),
    DEC("--", true, mapOf(Int to Int));

    override fun toString() = displayText
    fun accept(operand: MxType, lvalue: Boolean) =
        if (!lvalue && requireLvalue) null else when (operand) {
            is MxType.Primitive -> ret[operand]
            MxType.Null -> null
            MxType.Void -> null
            MxType.Unknown -> MxType.Unknown
            is MxType.Class -> null
            is MxType.Array -> null
        }
}
