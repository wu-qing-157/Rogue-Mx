package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.grammar.MxVariable
import personal.wuqing.rogue.ir.grammar.IRItem

object GlobalMap {
    private val map = mutableMapOf<MxVariable, IRItem.Global>()
    operator fun get(g: MxVariable) = map[g] ?: error("cannot find global variable")
    operator fun set(v: MxVariable, g: IRItem.Global) {
        map[v] = g
    }

    fun all() = map.values
    fun entries() = map.entries
}
