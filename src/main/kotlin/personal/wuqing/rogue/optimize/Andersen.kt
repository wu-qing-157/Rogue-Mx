package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

class Andersen private constructor(builder: Builder) {
    private val pointing: Map<IRItem, Set<IRItem>> = builder.pointing

    constructor(program: IRProgram) : this(Builder(program))

    operator fun get(item: IRItem) = pointing[item] ?: setOf()

    private class Builder(program: IRProgram) {
        val returns = program.function
            .associateWith { f -> f.body.mapNotNull { (it.terminate as? IRStatement.Terminate.Ret)?.item } }
        val pointing = mutableMapOf<IRItem, MutableSet<IRItem>>()
        val inclusive = mutableMapOf<IRItem, MutableSet<IRItem>>()
        val deLhs = mutableMapOf<IRItem, MutableSet<IRItem>>()
        val deRhs = mutableMapOf<IRItem, MutableSet<IRItem>>()
        val IRItem.pointing get() = this@Builder.pointing.computeIfAbsent(this) { mutableSetOf() }
        val IRItem.inclusive get() = this@Builder.inclusive.computeIfAbsent(this) { mutableSetOf() }
        val IRItem.deLhs get() = this@Builder.deLhs.computeIfAbsent(this) { mutableSetOf() }
        val IRItem.deRhs get() = this@Builder.deRhs.computeIfAbsent(this) { mutableSetOf() }
        val queue = LinkedList<IRItem>()

        init {
            program.global.forEach {
                it.pointing += IRItem.Local()
                queue += it
            }
            for (func in program.function) for (block in func.body) {
                for (st in block.phi) for (src in st.list.values) st.result.inclusive += src
                for (st in block.normal) when (st) {
                    is IRStatement.Normal.Load -> st.src.deLhs += st.dest
                    is IRStatement.Normal.Store -> st.dest.deRhs += st.src
                    is IRStatement.Normal.Alloca -> error("alloca in Andersen")
                    is IRStatement.Normal.ICalc ->
                        if (st.operator == IRCalcOp.ADD) {
                            st.op1.inclusive += st.result
                            st.op2.inclusive += st.result
                        } else if (st.operator == IRCalcOp.SUB) st.op1.inclusive += st.result
                    is IRStatement.Normal.ICmp -> Unit
                    is IRStatement.Normal.Call -> {
                        if (st.result != null &&
                            (st.function == IRFunction.Builtin.MallocArray ||
                                    st.function == IRFunction.Builtin.MallocObject)
                        ) {
                            st.result.pointing += IRItem.Local()
                            queue += st.result
                        } else if (st.function is IRFunction.Declared) {
                            if (st.result != null) returns[st.function]?.forEach { it.inclusive += st.result }
                            (st.args zip st.function.args).forEach { (a, b) -> a.inclusive += b }
                        }
                    }
                    IRStatement.Normal.NOP -> Unit
                }
            }

            while (queue.isNotEmpty()) queue.poll()?.let { node ->
                for (pt in node.pointing) {
                    for (lhs in node.deLhs) if (lhs !in pt.inclusive) {
                        pt.inclusive += lhs
                        queue += pt
                    }
                    for (rhs in node.deRhs) if (pt !in rhs.inclusive) {
                        rhs.inclusive += pt
                        queue += rhs
                    }
                }
                for (inc in node.inclusive) if (node.pointing.any { it !in inc.pointing }) {
                    inc.pointing += node.pointing
                    queue += inc
                }
            }
        }
    }
}
