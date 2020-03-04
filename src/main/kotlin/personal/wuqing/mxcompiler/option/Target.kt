package personal.wuqing.mxcompiler.option

enum class Target(private val description: String, val ext: String) {
    ALL("full compilation", ""),
    SEMANTIC("SEMANTIC", "?"),
    LLVM("LLVM", ".ll");

    override fun toString() = description
}
