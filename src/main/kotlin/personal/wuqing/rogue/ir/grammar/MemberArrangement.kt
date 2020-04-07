package personal.wuqing.rogue.ir.grammar

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.grammar.MxVariable
import personal.wuqing.rogue.ir.map.TypeMap

class MemberArrangement private constructor(
    val size: Int, val members: List<MxVariable>, val index: Map<MxVariable, Int>, val types: List<IRType>
) {
    private constructor(builder: Builder) : this(builder.size, builder.variables, builder.index, builder.types)

    constructor(clazz: MxType.Class) : this(Builder(clazz))

    private class Builder(clazz: MxType.Class) {
        val variables = clazz.variables.values.sortedByDescending { it.type.size }
        val size = (variables.sumBy { it.type.size } + 7) / 8 * 8
        val index = variables.zip(Array(variables.size) { it }).toMap()
        val types = variables.map { TypeMap[it.type] }
    }
}
