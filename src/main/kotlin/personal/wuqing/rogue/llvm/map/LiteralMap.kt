package personal.wuqing.rogue.llvm.map

import personal.wuqing.rogue.llvm.grammar.LLVMGlobal
import personal.wuqing.rogue.llvm.grammar.LLVMName
import personal.wuqing.rogue.llvm.grammar.LLVMType

object LiteralMap {
    private val map = mutableMapOf<String, LLVMGlobal>()
    private var count = 0
    operator fun get(s: String) = map[s] ?: LLVMGlobal(
        "__literal__.${count++}",
        LLVMType.Vector(s.toByteArray().size + 1, LLVMType.I8),
        LLVMName.Literal(s.length, "$s\u0000")
    ).also { map[s] = it }
    fun all() = map.values
}
