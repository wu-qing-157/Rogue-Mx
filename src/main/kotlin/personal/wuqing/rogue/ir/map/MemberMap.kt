package personal.wuqing.rogue.ir.map

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.grammar.MxVariable
import personal.wuqing.rogue.ir.grammar.MemberArrangement

object MemberMap {
    private val member = mutableMapOf<MxType.Class, MemberArrangement>()

    operator fun get(p: Pair<MxType.Class, MxVariable>) =
        member.computeIfAbsent(p.first) { MemberArrangement(it) }.index[p.second] ?: error("cannot find member")

    operator fun get(c: MxType.Class) = member.computeIfAbsent(c) {MemberArrangement(it)}.size
}
