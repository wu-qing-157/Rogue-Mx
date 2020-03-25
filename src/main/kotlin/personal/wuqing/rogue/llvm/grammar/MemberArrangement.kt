package personal.wuqing.rogue.llvm.grammar

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.grammar.MxVariable

class MemberArrangement private constructor(val size: Int, val members: List<MxVariable>, val delta: Map<MxVariable, Int>) {
    companion object {
        operator fun invoke(clazz: MxType.Class): MemberArrangement {
            val variables = clazz.variables.values
            val sorted = variables.sortedByDescending { it.type.size }
            val sum = sorted.sumBy { it.type.size }
            val delta = sorted.zip(Array(sorted.size) { it }).toMap()
            val allocSize = (sum + 7) / 8 * 8
            return MemberArrangement(allocSize, sorted, delta)
        }
    }
}
