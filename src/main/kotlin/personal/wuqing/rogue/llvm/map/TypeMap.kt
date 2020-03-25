package personal.wuqing.rogue.llvm.map

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.llvm.grammar.IRType
import personal.wuqing.rogue.llvm.grammar.MemberArrangement

object TypeMap {
    private val map = mutableMapOf<MxType, IRType>()
    operator fun get(t: MxType): IRType = map[t] ?: when (t) {
        MxType.Primitive.Int -> IRType.I32.also { map[t] = it }
        MxType.Primitive.Bool -> IRType.I1.also { map[t] = it }
        MxType.Primitive.String -> IRType.string.also { map[t] = it }
        MxType.Null -> IRType.Null.also { map[t] = it }
        MxType.Void -> IRType.Void.also { map[t] = it }
        MxType.Unknown -> throw Exception("type <unknown> found after semantic")
        is MxType.Class -> IRType.Class(t.name)
            .also { map[t] = IRType.Pointer(it) }
            .apply { init(MemberArrangement(t)) }
            .let { IRType.Pointer(it) }
        is MxType.Array -> IRType.Pointer(this[t.base]).also { map[t] = it }
    }
    fun all() = map.values
}
