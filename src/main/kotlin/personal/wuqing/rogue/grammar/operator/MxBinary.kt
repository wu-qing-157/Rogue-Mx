package personal.wuqing.rogue.grammar.operator

import personal.wuqing.rogue.grammar.MxType

enum class MxBinary(
    private val displayText: kotlin.String, private val requireLvalue: Pair<Boolean, Boolean>,
    val lvalue: Boolean, private val ret: Map<Pair<MxType.Primitive, MxType.Primitive>, Pair<MxType, Operation>>
) {
    PLUS(
        "+", false to false, false, mapOf(
            (Int to Int) to (Int to Operation.Plus), (String to String) to (String to Operation.SPlus)
        )
    ),
    MINUS("-", false to false, false, mapOf((Int to Int) to (Int to Operation.Minus))),
    TIMES("*", false to false, false, mapOf((Int to Int) to (Int to Operation.Times))),
    DIV("/", false to false, false, mapOf((Int to Int) to (Int to Operation.Div))),
    REM("%", false to false, false, mapOf((Int to Int) to (Int to Operation.Rem))),
    L_AND("&&", false to false, false, mapOf((Bool to Bool) to (Bool to Operation.BAnd))),
    L_OR("||", false to false, false, mapOf((Bool to Bool) to (Bool to Operation.BOr))),
    A_AND("&", false to false, false, mapOf((Int to Int) to (Int to Operation.IAnd))),
    A_OR("|", false to false, false, mapOf((Int to Int) to (Int to Operation.IOr))),
    A_XOR("^", false to false, false, mapOf((Int to Int) to (Int to Operation.Xor))),
    SHL("<<", false to false, false, mapOf((Int to Int) to (Int to Operation.Shl))),
    U_SHR(">>>", false to false, false, mapOf((Int to Int) to (Int to Operation.UShr))),
    SHR(">>", false to false, false, mapOf((Int to Int) to (Int to Operation.Shr))),
    LESS(
        "<", false to false, false, mapOf(
            (Int to Int) to (Bool to Operation.Less), (String to String) to (Bool to Operation.SLess)
        )
    ),
    LEQ(
        "<=", false to false, false, mapOf(
            (Int to Int) to (Bool to Operation.Leq), (String to String) to (Bool to Operation.SLeq)
        )
    ),
    GREATER(
        ">", false to false, false, mapOf(
            (Int to Int) to (Bool to Operation.Greater), (String to String) to (Bool to Operation.SGreater)
        )
    ),
    GEQ(
        ">=", false to false, false, mapOf(
            (Int to Int) to (Bool to Operation.Geq), (String to String) to (Bool to Operation.SGeq)
        )
    ),
    EQUAL(
        "==", false to false, false, mapOf(
            (Bool to Bool) to (Bool to Operation.BEqual),
            (Int to Int) to (Bool to Operation.IEqual),
            (String to String) to (Bool to Operation.SEqual)
        )
    ),
    NEQ(
        "!=", false to false, false, mapOf(
            (Bool to Bool) to (Bool to Operation.BNeq),
            (Int to Int) to (Bool to Operation.INeq),
            (String to String) to (Bool to Operation.SNeq)
        )
    ),
    ASSIGN(
        "=", true to false, true, mapOf(
            (Bool to Bool) to (MxType.Void to Operation.BAssign),
            (Int to Int) to (MxType.Void to Operation.IAssign),
            (String to String) to (MxType.Void to Operation.SAssign)
        )
    ),
    PLUS_I(
        "+=", true to false, true, mapOf(
            (Int to Int) to (Int to Operation.PlusI), (String to String) to (String to Operation.SPlusI)
        )
    ),
    MINUS_I("-=", true to false, true, mapOf((Int to Int) to (Int to Operation.MinusI))),
    TIMES_I("*=", true to false, true, mapOf((Int to Int) to (Int to Operation.TimesI))),
    DIV_I("/=", true to false, true, mapOf((Int to Int) to (Int to Operation.DivI))),
    REM_I("%=", true to false, true, mapOf((Int to Int) to (Int to Operation.RemI))),
    AND_I("&=", true to false, true, mapOf((Int to Int) to (Int to Operation.AndI))),
    OR_I("|=", true to false, true, mapOf((Int to Int) to (Int to Operation.OrI))),
    XOR_I("^=", true to false, true, mapOf((Int to Int) to (Int to Operation.XorI))),
    SHL_I("<<=", true to false, true, mapOf((Int to Int) to (Int to Operation.ShlI))),
    U_SHR_I(">>>=", true to false, true, mapOf((Int to Int) to (Int to Operation.UShrI))),
    SHR_I(">>=", true to false, true, mapOf((Int to Int) to (Int to Operation.ShrI)));

    override fun toString() = displayText
    fun accept(lhs: Pair<MxType, Boolean>, rhs: Pair<MxType, Boolean>): MxType? {
        if (!lhs.second && requireLvalue.first) return null
        if (!rhs.second && requireLvalue.second) return null
        val (l, r) = lhs.first to rhs.first
        return when {
            l is MxType.Unknown || r is MxType.Unknown -> MxType.Unknown
            l is MxType.Void || r is MxType.Void -> null
            l is MxType.Primitive && r is MxType.Primitive -> ret[l to r]?.first
            this == ASSIGN && (l is MxType.Class || l is MxType.Array) && (r == l || r is MxType.Null) -> l
            (this == EQUAL || this == NEQ) && (l == r || l is MxType.Null || r is MxType.Null) -> Bool
            else -> null
        }
    }

    fun operation(lhs: MxType, rhs: MxType) = when {
        lhs is MxType.Primitive && rhs is MxType.Primitive -> ret[lhs to rhs]?.second
        this == ASSIGN && (lhs is MxType.Class || lhs is MxType.Array) && (rhs == lhs || rhs is MxType.Null) ->
            Operation.PAssign(lhs)
        this == EQUAL && (lhs is MxType.Class || lhs is MxType.Array) && (lhs == rhs || rhs is MxType.Null) ->
            Operation.PEqual(lhs)
        this == EQUAL && (lhs == rhs && lhs is MxType.Null) && (rhs is MxType.Class || rhs is MxType.Array) ->
            Operation.PEqual(lhs)
        this == NEQ && (lhs is MxType.Class || lhs is MxType.Array) && (lhs == rhs || rhs is MxType.Null) ->
            Operation.PNeq(rhs)
        this == NEQ && (lhs == rhs && lhs is MxType.Null) && (rhs is MxType.Class || rhs is MxType.Array) ->
            Operation.PNeq(rhs)
        else -> null
    }
}
