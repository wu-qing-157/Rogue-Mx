package personal.wuqing.mxcompiler.grammar

private typealias Int = Type.Primitive.Int
private typealias Bool = Type.Primitive.Bool
private typealias String = Type.Primitive.String

enum class SuffixOperator(
    private val displayText: kotlin.String, private val requireLvalue: Boolean,
    private val ret: Map<Type.Primitive, Type.Primitive>
) {
    INC("++", true, mapOf(Int to Int)),
    DEC("--", true, mapOf(Int to Int));

    override fun toString() = displayText
    fun accept(operand: Type, lvalue: Boolean) =
        if (!lvalue && requireLvalue) null else when (operand) {
        is Type.Primitive -> ret[operand]
        Type.Null -> null
        Type.Void -> null
        Type.Unknown -> Type.Unknown
        is Type.Class -> null
        is Type.Array -> null
    }
}

enum class PrefixOperator(
    private val displayText: kotlin.String, private val requireLvalue: Boolean,
    val lvalue: Boolean, private val ret: Map<Type.Primitive, Type.Primitive>
) {
    INC("++", true, true, mapOf(Int to Int)),
    DEC("--", true, true, mapOf(Int to Int)),
    L_NEG("!", false, false, mapOf(Bool to Bool)),
    INV("~", false, false, mapOf(Int to Int)),
    POS("+", false, false, mapOf(Int to Int)),
    NEG("-", false, false, mapOf(Int to Int));

    override fun toString() = displayText
    fun accept(operand: Type, lvalue: Boolean) =
        if (!lvalue && requireLvalue) null else when (operand) {
            is Type.Primitive -> ret[operand]
            Type.Null -> null
            Type.Void -> null
            Type.Unknown -> Type.Unknown
            is Type.Class -> null
            is Type.Array -> null
        }
}

enum class BinaryOperator(
    private val displayText: kotlin.String, private val requireLvalue: Pair<Boolean, Boolean>,
    val lvalue: Boolean, private val ret: Map<Pair<Type.Primitive, Type.Primitive>, Pair<Type.Primitive, Operation>>
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
            (Bool to Bool) to (Bool to Operation.BAssign),
            (Int to Int) to (Bool to Operation.IAssign),
            (String to String) to (Bool to Operation.SAssign)
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
    fun accept(lhs: Pair<Type, Boolean>, rhs: Pair<Type, Boolean>): Type? {
        if (!lhs.second && requireLvalue.first) return null
        if (!rhs.second && requireLvalue.second) return null
        val (l, r) = lhs.first to rhs.first
        return when {
            l is Type.Unknown || r is Type.Unknown -> Type.Unknown
            l is Type.Void || r is Type.Void -> null
            l is Type.Primitive && r is Type.Primitive -> ret[l to r]?.first
            this == ASSIGN && (l is Type.Class || l is Type.Array) && (r == l || r is Type.Null) -> l
            (this == EQUAL || this == NEQ) && (l == r || l is Type.Null || r is Type.Null) -> Bool
            else -> null
        }
    }

    fun operation(lhs: Type, rhs: Type) = when {
        lhs is Type.Primitive && rhs is Type.Primitive -> ret[lhs to rhs]?.second
        this == ASSIGN && (lhs is Type.Class || lhs is Type.Array) && (rhs == lhs || rhs is Type.Null) ->
            Operation.PAssign(lhs)
        this == EQUAL && (lhs is Type.Class || lhs is Type.Array) && (lhs == rhs || rhs is Type.Null) ->
            Operation.PEqual(lhs)
        this == EQUAL && (lhs == rhs && lhs is Type.Null) && (rhs is Type.Class || rhs is Type.Array) ->
            Operation.PEqual(lhs)
        this == NEQ && (lhs is Type.Class || lhs is Type.Array) && (lhs == rhs || rhs is Type.Null) ->
            Operation.PNeq(rhs)
        this == NEQ && (lhs == rhs && lhs is Type.Null) && (rhs is Type.Class || rhs is Type.Array) ->
            Operation.PNeq(rhs)
        else -> null
    }
}

sealed class Operation {
    object Plus : Operation()
    object SPlus : Operation()
    object Minus : Operation()
    object Times : Operation()
    object Div : Operation()
    object Rem : Operation()
    object BAnd : Operation()
    object BOr : Operation()
    object IAnd : Operation()
    object IOr : Operation()
    object Xor : Operation()
    object Shl : Operation()
    object UShr : Operation()
    object Shr : Operation()
    object Less : Operation()
    object SLess : Operation()
    object Leq : Operation()
    object SLeq : Operation()
    object Greater : Operation()
    object SGreater : Operation()
    object Geq : Operation()
    object SGeq : Operation()
    object BEqual : Operation()
    object IEqual : Operation()
    object SEqual : Operation()
    object BNeq : Operation()
    object INeq : Operation()
    object SNeq : Operation()
    object BAssign : Operation()
    object IAssign : Operation()
    object SAssign : Operation()
    object PlusI : Operation()
    object SPlusI : Operation()
    object MinusI : Operation()
    object TimesI : Operation()
    object DivI : Operation()
    object RemI : Operation()
    object AndI : Operation()
    object OrI : Operation()
    object XorI : Operation()
    object ShlI : Operation()
    object UShrI : Operation()
    object ShrI : Operation()
    class PEqual(val clazz: Type) : Operation()
    class PNeq(val clazz: Type) : Operation()
    class PAssign(val clazz: Type) : Operation()
}
