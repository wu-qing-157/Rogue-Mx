package personal.wuqing.rogue.llvm.grammar

class IRGlobal constructor(val name: IRItem, val value: IRItem) {
    override fun toString() = "${name.display} = private global ${value.type} ${value.display}"
}
