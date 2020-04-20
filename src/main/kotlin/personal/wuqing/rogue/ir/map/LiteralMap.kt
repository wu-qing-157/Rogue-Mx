package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.ir.grammar.IRItem

object LiteralMap {
    private val map = mutableMapOf<String, IRItem.Literal>()
    operator fun get(s: String) = map.computeIfAbsent(s) { IRItem.Literal(s) }
    fun all() = map.values
}
