package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.utils.DomTree

object CommonSubexpressionElimination {
    private class Expression(val st: IRStatement.WithResult, val block: IRBlock) {
        override fun equals(other: Any?) = other is Expression && st == other.st
        override fun hashCode() = st.hashCode()
        override fun toString() = st.toString()
    }

    private val alias = mutableMapOf<IRItem, IRItem>()
    private val IRItem.alias: IRItem
        get() = CommonSubexpressionElimination.alias[this]?.let {
            it.alias.also { new -> CommonSubexpressionElimination.alias[this] = new }
        } ?: this

    private fun alias(a: IRItem, b: IRItem) {
        if (a.alias != b.alias) alias[b.alias] = a.alias
    }

    private fun same(a: IRItem?, b: IRItem?) =
        if (a is IRItem && b is IRItem) a.alias == b.alias
        else a == b

    private fun same(
        s: Expression, t: Expression, andersen: Andersen?, analysis: FunctionCallAnalysis, storeInBlock: Set<IRItem>
    ): Boolean {
        val a = s.st
        val b = t.st
        return when {
            a is IRStatement.Phi && b is IRStatement.Phi ->
                b.list.keys.all { it in a.list } && a.list.entries.all { (k, v) -> same(v, b.list[k]) }
            a is IRStatement.Normal.ICalc && b is IRStatement.Normal.ICalc ->
                a.operator == b.operator && same(a.op1, b.op1) && same(a.op2, b.op2)
            a is IRStatement.Normal.Call && b is IRStatement.Normal.Call ->
                a.function == b.function && !analysis.sideEffect(a.function) && andersen != null &&
                        (a.args zip b.args).all { (u, v) -> same(u, v) } &&
                        check(s.block, t.block, analysis.loaded(a.function), storeInBlock)
            a is IRStatement.Normal.Load && b is IRStatement.Normal.Load ->
                andersen != null && same(a.src, b.src) && check(s.block, t.block, andersen[a.src], storeInBlock)
            else -> false
        }
    }

    private fun <T> disjoint(a: Set<T>, b: Set<T>) = a.none { it in b } && b.none { it in a }

    private fun check(s: IRBlock, t: IRBlock, conflict: Set<IRItem>, storeInBlock: Set<IRItem>): Boolean {
        if (s == t) return true
        if (!disjoint(storeInBlock, conflict)) return false
        val visited = mutableSetOf<IRBlock>()
        val failed = mutableSetOf<IRBlock>()
        var gg = false
        fun visit(cur: IRBlock, fail: Boolean) {
            if (cur == s) return
            if (cur == t && fail) gg = true
            if (fail) {
                if (cur in failed) return
                else failed += cur
            } else {
                if (cur in visited) return
                else visited += cur
            }
            for (next in cur.next) visit(next, fail || !disjoint(conflict, cur.stored))
        }
        for (next in s.next) visit(next, false)
        return !gg
    }

    private val stored = mutableMapOf<IRBlock, MutableSet<IRItem>>()
    private val IRBlock.stored get() = CommonSubexpressionElimination.stored.computeIfAbsent(this) { mutableSetOf() }

    private fun move(dest: IRItem.Local, src: IRItem?): IRStatement.Normal.ICalc {
        alias(dest, src ?: error("no result"))
        return IRStatement.Normal.ICalc(dest, IRCalcOp.ADD, src, IRItem.Const(0))
    }

    private fun process(func: IRFunction.Declared, andersen: Andersen?, analysis: FunctionCallAnalysis) {
        alias.clear()
        stored.clear()
        for (block in func.body) {
            for (st in block.phi) {
                val one = st.list.values.first()
                if (st.list.values.all { same(it, one) }) alias(st.result, one)
            }
            for (st in block.normal) {
                if (st is IRStatement.Normal.ICalc && st.operator == IRCalcOp.ADD && st.op2 == IRItem.Const(0))
                    alias(st.result, st.op1)
                if (st is IRStatement.Normal.Store && andersen != null) block.stored += andersen[st.dest]
                if (st is IRStatement.Normal.Call) block.stored += analysis.stored(st.function)
            }
        }
        func.updatePrev()
        val domTree = DomTree(func.body[0])
        fun visit(block: IRBlock, expression: MutableList<Expression>) {
            val enterExpressionLength = expression.size
            val phiSubstitute = mutableMapOf<IRStatement.Phi, IRStatement.Normal>()
            val normalSubstitute = mutableMapOf<IRStatement.Normal, IRStatement.Normal>()
            val loads = mutableSetOf<IRStatement.Normal.Load>()
            val storeInBlock = mutableSetOf<IRItem>()
            for (st in block.phi) expression.lastOrNull {
                same(it, Expression(st, block), andersen, analysis, storeInBlock)
            }?.let { phiSubstitute[st] = move(st.result, it.st.result) } ?: run {
                val one = st.list.values.first()
                if (st.list.values.all { it == one }) phiSubstitute[st] = move(st.result, one)
                else expression += Expression(st, block)
            }
            for (st in block.normal) {
                if (st is IRStatement.WithResult) {
                    expression.lastOrNull {
                        same(it, Expression(st, block), andersen, analysis, storeInBlock)
                    }?.let {
                        if (st.result != null) normalSubstitute[st] = move(st.result!!, it.st.result)
                    } ?: run {
                        expression += Expression(st, block)
                        if (st is IRStatement.Normal.Load) loads += st
                    }
                }
                if (st is IRStatement.Normal.Call && andersen != null) {
                    for (load in loads.filter { !disjoint(andersen[it.src], analysis.stored(st.function)) }) {
                        expression -= Expression(load, block)
                        loads -= load
                    }
                    storeInBlock += analysis.stored(st.function)
                }
                if (st is IRStatement.Normal.Store && andersen != null) {
                    for (load in loads.filter { !disjoint(andersen[it.src], andersen[st.dest]) }) {
                        expression -= Expression(load, block)
                        loads -= load
                    }
                    storeInBlock += andersen[st.dest]
                    if (st.src is IRItem.Local) IRStatement.Normal.Load(st.src, st.dest).let {
                        loads += it
                        expression += Expression(it, block)
                    }
                }
            }
            if (phiSubstitute.isNotEmpty()) {
                block.phi -= phiSubstitute.keys
                block.normal.addAll(0, phiSubstitute.values)
            }
            block.normal.replaceAll { normalSubstitute[it] ?: it }
            for (child in domTree.child[block] ?: listOf()) visit(child, expression)
            while (expression.size > enterExpressionLength) expression.removeAt(expression.size - 1)
        }
        visit(func.body[0], mutableListOf())
    }

    operator fun invoke(program: IRProgram, useAndersen: Boolean = true) {
        val andersen = if (useAndersen) Andersen(program) else null
        val analysis = FunctionCallAnalysis(program, andersen)
        program.function.forEach { process(it, andersen, analysis) }
    }
}
