package personal.wuqing.mxcompiler.option

enum class Target(private val description: String, val ext: String) {
    ALL("full compilation", ""),
    AST("AST", ".ast"),
    SEMANTIC("SEMANTIC", "?"),
    IR("IR", ".ir");

    override fun toString() = description
}
