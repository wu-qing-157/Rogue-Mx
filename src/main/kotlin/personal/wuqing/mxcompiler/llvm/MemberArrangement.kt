package personal.wuqing.mxcompiler.llvm

import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.grammar.Variable

class MemberArrangement private constructor(val size: Int, val members: List<Variable>, val delta: Map<Variable, Int>) {
    companion object {
        operator fun invoke(clazz: Type.Class): MemberArrangement {
            val variables = clazz.variables.values
            val sorted = variables.sortedByDescending { it.type.size }
            val sum = sorted.sumBy { it.type.size }
            val delta = sorted.zip(Array(sorted.size) { it }).toMap()
            val allocSize = (sum + 7) / 8 * 8
            return MemberArrangement(allocSize, sorted, delta)
        }
    }
}
