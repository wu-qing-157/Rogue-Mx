package personal.wuqing.rogue.llvm.grammar

class IRBlock constructor(val name: String) {
    override fun toString() = "%$name"
    val statements = mutableListOf<IRStatement>()
}
