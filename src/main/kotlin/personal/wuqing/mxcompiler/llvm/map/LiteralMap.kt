package personal.wuqing.mxcompiler.llvm.map

import personal.wuqing.mxcompiler.llvm.grammar.LLVMGlobal
import personal.wuqing.mxcompiler.llvm.grammar.LLVMName
import personal.wuqing.mxcompiler.llvm.grammar.LLVMType

object LiteralMap {
    private val map = mutableMapOf<String, LLVMGlobal>()
    private var count = 0
    operator fun get(s: String) = map[s] ?: LLVMGlobal(
        "__literal__.${count++}", LLVMType.Vector(s.length + 5, LLVMType.I8), LLVMName.Literal(s.length, "$s\u0000")
    ).also { map[s] = it }
    fun all() = map.values
}
