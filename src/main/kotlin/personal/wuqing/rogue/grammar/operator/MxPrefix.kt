package personal.wuqing.rogue.grammar.operator

import personal.wuqing.rogue.grammar.MxType

enum class MxPrefix(
    private val displayText: kotlin.String, private val requireLvalue: Boolean,
    val lvalue: Boolean, private val ret: Map<MxType.Primitive, MxType.Primitive>
) {
    INC("++", true, true, mapOf(Int to Int)),
    DEC("--", true, true, mapOf(Int to Int)),
    L_NEG("!", false, false, mapOf(Bool to Bool)),
    INV("~", false, false, mapOf(Int to Int)),
    POS("+", false, false, mapOf(Int to Int)),
    NEG("-", false, false, mapOf(Int to Int));

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
