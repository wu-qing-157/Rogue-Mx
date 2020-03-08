package personal.wuqing.mxcompiler.llvm.map

import personal.wuqing.mxcompiler.grammar.MxVariable
import personal.wuqing.mxcompiler.llvm.grammar.LLVMGlobal

object GlobalMap {
    private val map = mutableMapOf<MxVariable, LLVMGlobal>()
    operator fun get(g: MxVariable) = map[g] ?: throw Exception("cannot find global variable")
    operator fun set(v: MxVariable, g: LLVMGlobal) {
        map[v] = g
    }

    fun all() = map.values
    fun entries() = map.entries
}
