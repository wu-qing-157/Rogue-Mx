package personal.wuqing.rogue.option

enum class Target(private val description: String, val ext: String) {
    ALL("full compilation", "s"),
    SEMANTIC("SEMANTIC", "?"),
    LLVM("LLVM", "ll");

    override fun toString() = description
}
