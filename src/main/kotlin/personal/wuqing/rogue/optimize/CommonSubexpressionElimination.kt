package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.utils.DomTree
import java.util.LinkedList

object CommonSubexpressionElimination {
    private val callerMap = mutableMapOf<IRFunction, MutableList<IRFunction>>()
    private val sideEffect = mutableSetOf<IRFunction>()
    private val sideEffectQueue = LinkedList<IRFunction>()

    private fun analyzeCaller(function: IRFunction.Declared) {
        for (block in function.body) for (inst in block.normal.filterIsInstance<IRStatement.Normal.Call>())
            callerMap.computeIfAbsent(inst.function) { mutableListOf() } += function
    }

    private fun analyzeSideEffect(program: IRProgram) {
        sideEffect.clear()
        sideEffectQueue.clear()
        sideEffect += listOf(
            IRFunction.Builtin.MallocObject, IRFunction.Builtin.MallocArray,
            IRFunction.Builtin.GetInt, IRFunction.Builtin.GetString,
            IRFunction.Builtin.Print, IRFunction.Builtin.Println,
            IRFunction.Builtin.PrintInt, IRFunction.Builtin.PrintlnInt
        )
        sideEffect += program.function.filter { f ->
            f.body.any { b -> b.normal.any { it is IRStatement.Normal.Store } }
        }
        sideEffectQueue += sideEffect
        while (sideEffectQueue.isNotEmpty()) sideEffectQueue.poll()?.let { old ->
            callerMap[old]?.let {
                sideEffectQueue += it - sideEffect
                sideEffect += it
            }
        }
    }

    private val alias = mutableMapOf<IRItem.Local, IRItem.Local>()
    private val IRItem.Local.alias: IRItem.Local
        get() = CommonSubexpressionElimination.alias[this]?.let {
            it.alias.also { new -> CommonSubexpressionElimination.alias[this] = new }
        } ?: this

    private fun alias(a: IRItem.Local, b: IRItem.Local) {
        alias[b.alias] = a.alias
    }

    private fun clear() {
        callerMap.clear()
        sideEffect.clear()
        sideEffectQueue.clear()
    }

    private fun same(a: IRItem?, b: IRItem?) =
        if (a is IRItem.Local && b is IRItem.Local) a.alias == b.alias
        else a == b

    private fun same(a: IRStatement.WithResult, b: IRStatement.WithResult) =
        when {
            a is IRStatement.Phi && b is IRStatement.Phi ->
                b.list.keys.all { it in a.list } && a.list.entries.all { (k, v) -> same(v, b.list[k]) }
            a is IRStatement.Normal.ICalc && b is IRStatement.Normal.ICalc ->
                a.operator == b.operator && same(a.op1, b.op1) && same(a.op2, b.op2)
            a is IRStatement.Normal.Call && b is IRStatement.Normal.Call ->
                a.function == b.function && a.function !in sideEffect &&
                        (a.args zip b.args).all { (u, v) -> same(u, v) }
            else -> false
        }

    private fun move(dest: IRItem.Local, src: IRItem.Local?): IRStatement.Normal.ICalc {
        alias(dest, src ?: error("no result"))
        return IRStatement.Normal.ICalc(dest, IRCalcOp.ADD, src, IRItem.Const(0))
    }

    private fun process(func: IRFunction.Declared) {
        alias.clear()
        val domTree = DomTree(func.body[0])
        fun visit(block: IRBlock, expression: MutableList<IRStatement.WithResult>) {
            val phiSubstitute = mutableMapOf<IRStatement.Phi, IRStatement.Normal>()
            val normalSubstitute = mutableMapOf<IRStatement.Normal, IRStatement.Normal>()
            for (st in block.phi) expression.lastOrNull { same(it, st) }?.let {
                phiSubstitute[st] = move(st.result, it.result)
            } ?: run {
                val one = st.list.values.first()
                if (one is IRItem.Local && st.list.values.all { it == one }) phiSubstitute[st] = move(st.result, one)
                else expression += st
            }
            for (st in block.normal) if (st is IRStatement.WithResult)
                expression.lastOrNull { same(it, st) }?.let {
                    if (st.result != null) normalSubstitute[st] = move(st.result!!, it.result)
                } ?: run { expression += st }
            if (phiSubstitute.isNotEmpty()) {
                block.phi -= phiSubstitute.keys
                block.normal.addAll(0, phiSubstitute.values)
            }
            block.normal.replaceAll { normalSubstitute[it] ?: it }
            for (child in domTree.child[block] ?: listOf()) visit(child, expression.toMutableList())
        }
        visit(func.body[0], mutableListOf())
    }

    operator fun invoke(program: IRProgram) {
        clear()
        program.function.forEach(::analyzeCaller)
        analyzeSideEffect(program)
        program.function.forEach(::process)
    }
}
