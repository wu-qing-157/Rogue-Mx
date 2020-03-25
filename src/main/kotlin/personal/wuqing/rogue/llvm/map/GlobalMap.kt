package personal.wuqing.rogue.llvm.map

import personal.wuqing.rogue.grammar.MxVariable
import personal.wuqing.rogue.llvm.grammar.IRGlobal

object GlobalMap {
    private val map = mutableMapOf<MxVariable, IRGlobal>()
    operator fun get(g: MxVariable) = map[g] ?: error("cannot find global variable")
    operator fun set(v: MxVariable, g: IRGlobal) {
        map[v] = g
    }

    fun all() = map.values
    fun entries() = map.entries
}
