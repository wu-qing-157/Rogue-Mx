package personal.wuqing.rogue.llvm.map

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.llvm.grammar.LLVMType
import personal.wuqing.rogue.llvm.grammar.MemberArrangement

object TypeMap {
    private val map = mutableMapOf<MxType, LLVMType>()
    operator fun get(t: MxType): LLVMType = map[t] ?: when (t) {
        MxType.Primitive.Int -> LLVMType.I32.also { map[t] = it }
        MxType.Primitive.Bool -> LLVMType.I1.also { map[t] = it }
        MxType.Primitive.String -> LLVMType.string.also { map[t] = it }
        MxType.Null -> LLVMType.Null.also { map[t] = it }
        MxType.Void -> LLVMType.Void.also { map[t] = it }
        MxType.Unknown -> throw Exception("type <unknown> found after semantic")
        is MxType.Class -> LLVMType.Class(t.name)
            .also { map[t] = LLVMType.Pointer(it) }
            .apply { init(MemberArrangement(t)) }
            .let { LLVMType.Pointer(it) }
        is MxType.Array -> LLVMType.Pointer(this[t.base]).also { map[t] = it }
    }
    fun all() = map.values
}
