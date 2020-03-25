package personal.wuqing.rogue.llvm.map

import personal.wuqing.rogue.llvm.grammar.IRGlobal
import personal.wuqing.rogue.llvm.grammar.IRItem
import personal.wuqing.rogue.llvm.grammar.IRType

object LiteralMap {
    private val map = mutableMapOf<String, IRGlobal>()
    private var count = 0
    operator fun get(s: String) = map[s] ?: IRItem.Literal(s).let {
        IRGlobal(IRItem.Global(IRType.Pointer(it.type), "literal.${count++}"), it)
    }.also { map[s] = it }
    fun all() = map.values
}
