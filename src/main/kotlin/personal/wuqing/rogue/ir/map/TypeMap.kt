package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.ir.grammar.IRType

object TypeMap {
    private val map = mutableMapOf<MxType.Class, IRType.Class>()
    operator fun get(t: MxType): IRType = when (t) {
        MxType.Primitive.Int -> IRType.I32
        MxType.Primitive.Bool -> IRType.I1
        MxType.Primitive.String -> IRType.String
        MxType.Null -> IRType.Null
        MxType.Void -> IRType.Void
        MxType.Unknown -> error("unknown type when building IR")
        is MxType.Class -> map.computeIfAbsent(t) { IRType.Class(t, t.name) }
        is MxType.Array -> IRType.Array(this[t.base])
    }
    fun all() = map.values
}
