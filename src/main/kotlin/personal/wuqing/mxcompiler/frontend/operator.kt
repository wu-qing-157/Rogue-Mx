package personal.wuqing.mxcompiler.frontend

enum class SuffixOperator(private val displayText: String) {
    INC("++"), DEC("--");

    override fun toString() = displayText
}

enum class PrefixOperator(private val displayText: String, val lvalue: Boolean) {
    INC("++", true), DEC("--", true),
    LOGIC_NEGATION("!", false), ARITHMETIC_NEGATION("~", false),
    POSITIVE("+", false), NEGATIVE("-", false);

    override fun toString() = displayText
}

enum class BinaryOperator(private val displayText: String, val lvalue: Boolean) {
    PLUS("+", false), MINUS("-", false),
    TIMES("*", false), DIVIDE("/", false), REM("%", false),
    LOGIC_AND("&&", false), LOGIC_OR("||", false),
    ARITHMETIC_AND("&", false), ARITHMETIC_OR("|", false),
    ARITHMETIC_XOR("^", false),
    SHIFT_LEFT("<<", false),
    LOGIC_SHIFT_RIGHT(">>>", false), ARITHMETIC_SHIFT_RIGHT(">>", false),
    LESS("<", false), LESS_EQUAL("<=", false),
    GREATER(">", false), GREATER_EQUAL(">=", false),
    EQUAL("==", false), NOT_EQUAL("!=", false),
    ASSIGN("=", true),
    PLUS_ASSIGN("+=", true), MINUS_ASSIGN("-=", true),
    TIMES_ASSIGN("*=", true), DIVIDE_ASSIGN("/=", true),
    REM_ASSIGN("%=", true),
    AND_ASSIGN("&=", true), OR_ASSIGN("|=", true),
    XOR_ASSIGN("^=", true),
    SHIFT_LEFT_ASSIGN("<<=", true),
    LOGIC_SHIFT_RIGHT_ASSIGN(">>>=", true),
    ARITHMETIC_SHIFT_RIGHT_ASSIGN(">>=", true);

    override fun toString() = displayText
}
