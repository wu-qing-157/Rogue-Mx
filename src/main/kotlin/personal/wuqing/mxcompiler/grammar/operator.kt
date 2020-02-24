package personal.wuqing.mxcompiler.grammar

enum class SuffixOperator(private val displayText: String) {
    INC("++"), DEC("--");

    override fun toString() = displayText
}

enum class PrefixOperator(private val displayText: String, val lvalue: Boolean) {
    INC("++", true), DEC("--", true),
    L_NEG("!", false), INV("~", false),
    POS("+", false), NEG("-", false);

    override fun toString() = displayText
}

enum class BinaryOperator(private val displayText: String, val lvalue: Boolean) {
    PLUS("+", false), MINUS("-", false),
    TIMES("*", false), DIV("/", false), REM("%", false),
    L_AND("&&", false), L_OR("||", false),
    A_AND("&", false), A_OR("|", false),
    A_XOR("^", false),
    SHL("<<", false),
    U_SHR(">>>", false), SHR(">>", false),
    LESS("<", false), LEQ("<=", false),
    GREATER(">", false), GEQ(">=", false),
    EQUAL("==", false), UNEQUAL("!=", false),
    ASSIGN("=", true),
    PLUS_I("+=", true), MINUS_I("-=", true),
    TIMES_I("*=", true), DIV_I("/=", true),
    REM_I("%=", true),
    AND_I("&=", true), OR_I("|=", true),
    XOR_I("^=", true),
    SHL_I("<<=", true),
    U_SHR_I(">>>=", true), SHR_I(">>=", true);

    override fun toString() = displayText
}
