package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.grammar.IRType
import personal.wuqing.rogue.utils.DomTree
import java.util.LinkedList

object Mem2Reg {
    private class Variable(val type: IRType)

    private var cnt = 0
    private fun next(prefix: String, type: IRType) = IRItem.Local(type, "$prefix.${cnt++}")

    private val variables = mutableMapOf<IRItem, Variable>()
    private val assigned = mutableMapOf<Variable, MutableSet<IRBlock>>()
    private val contains = mutableMapOf<IRBlock, MutableSet<Variable>>()
    private val need = mutableMapOf<IRBlock, MutableSet<Variable>>()
    private val result = mutableMapOf<Pair<IRBlock, Variable>, IRItem.Local>()
    private val phi = mutableMapOf<Pair<IRBlock, Variable>, MutableMap<IRBlock, IRItem>>()
    private val names = mutableMapOf<Variable, LinkedList<IRItem>>()
    private val alias = mutableMapOf<IRItem, IRItem>()

    private fun clear() {
        cnt = 0
        variables.clear()
        assigned.clear()
        contains.clear()
        need.clear()
        result.clear()
        phi.clear()
        names.clear()
        alias.clear()
    }

    private fun computeAssign(blocks: Iterable<IRBlock>) {
        blocks.forEach { block ->
            block.normal.filterIsInstance<IRStatement.Normal.Alloca>().forEach { state ->
                variables[state.item] = Variable(state.item.type)
                    .also { assigned[it] = mutableSetOf() }
            }
            contains[block] = mutableSetOf()
            need[block] = mutableSetOf()
        }
        blocks.forEach { block ->
            block.normal.filterIsInstance<IRStatement.Normal.Store>().forEach { state ->
                variables[state.dest]?.let {
                    assigned[it]!! += block
                    contains[block]!! += it
                }
            }
        }
    }

    private fun generatePhi(tree: DomTree<IRBlock>, prefix: String) {
        val frontier = tree.frontier()
        for ((a, sites) in assigned) {
            val queue = LinkedList(sites)
            while (queue.isNotEmpty()) {
                val n = queue.poll()
                for (y in frontier[n] ?: error("cannot find node on dominator tree")) if (a !in need[y]!!) {
                    need[y]!! += a
                    result[y to a] = next(prefix, (a.type as IRType.Pointer).base)
                    phi[y to a] = mutableMapOf()
                    if (y !in sites) queue.add(y)
                }
            }
        }
    }

    private fun initializeNames() {
        for ((i, v) in variables) {
            names[v] = LinkedList<IRItem>().apply {
                add(
                    when (val t = (i.type as? IRType.Pointer ?: error("variable in llvm is not pointer")).base) {
                        IRType.I32 -> IRType.I32 const 0
                        IRType.I8 -> IRType.I8 const 0
                        IRType.I1 -> IRType.I1 const 0
                        is IRType.Pointer -> IRItem.Null(t)
                        else -> error("variable in llvm points to illegal type")
                    }
                )
            }
        }
    }

    private fun newName(i: IRItem): IRItem = alias[i]?.let { newName(it) } ?: i

    private fun rename(n: IRBlock, tree: DomTree<IRBlock>) {
        val track = mutableListOf<Variable>()
        for (v in need[n]!!) {
            track += v
            names[v]!! += result[n to v]!!
        }
        n.normal.apply {
            replaceAll {
                when (it) {
                    is IRStatement.Normal.Load ->
                        variables[it.src]?.let { variable ->
                            IRStatement.Normal.Nop.also { _ -> alias[it.dest] = names[variable]!!.last }
                        } ?: it
                    is IRStatement.Normal.Store -> variables[it.dest]?.let { variable ->
                        track += variable
                        names[variable]!! += it.src
                        IRStatement.Normal.Nop
                    } ?: it
                    is IRStatement.Normal.Alloca -> IRStatement.Normal.Nop
                    else -> it
                }
            }
            removeAll { it is IRStatement.Normal.Nop }
        }
        for (y in n.next) for (a in need[y]!!) phi[y to a]!![n] = names[a]!!.last
        for (x in tree.child[n] ?: error("cannot find node on dominator tree")) rename(x, tree)
        for (v in track) names[v]!!.removeLast()
    }

    private fun eliminateAlias(n: IRBlock) {
        n.phi.replaceAll { IRStatement.Phi(it.result, it.list.map { (i, b) -> newName(i) to b }) }
        n.normal.replaceAll {
            when (it) {
                is IRStatement.Normal.Load -> it
                is IRStatement.Normal.Store ->
                    IRStatement.Normal.Store(dest = newName(it.dest), src = newName(it.src))
                is IRStatement.Normal.Alloca -> IRStatement.Normal.Nop
                is IRStatement.Normal.ICalc -> IRStatement.Normal.ICalc(
                    result = it.result, operator = it.operator,
                    op1 = newName(it.op1), op2 = newName(it.op2)
                )
                is IRStatement.Normal.ICmp -> IRStatement.Normal.ICmp(
                    result = it.result, operator = it.operator,
                    op1 = newName(it.op1), op2 = newName(it.op2)
                )
                is IRStatement.Normal.Call -> IRStatement.Normal.Call(
                    result = it.result, function = it.function,
                    args = it.args.map { arg -> newName(arg) }
                )
                is IRStatement.Normal.Cast -> IRStatement.Normal.Cast(
                    from = newName(it.from), to = it.to
                )
                is IRStatement.Normal.Element -> IRStatement.Normal.Element(
                    result = it.result, src = newName(it.src),
                    indices = it.indices.map { index -> newName(index) }
                )
                IRStatement.Normal.Nop -> TODO()
            }
        }
        n.terminate = when (val old = n.terminate) {
            is IRStatement.Terminate.Ret -> IRStatement.Terminate.Ret(newName(old.item))
            is IRStatement.Terminate.Branch -> IRStatement.Terminate.Branch(
                cond = newName(old.cond), els = old.els, then = old.then
            )
            is IRStatement.Terminate.Jump -> old
        }
    }

    private operator fun invoke(function: IRFunction.Declared, prefix: String) {
        clear()
        val tree = DomTree(function.body.first())
        val blocks = tree.child.keys
        function.body.removeIf { it !in blocks }
        computeAssign(blocks)
        generatePhi(tree, prefix)
        initializeNames()
        rename(tree.root, tree)
        for ((key, entry) in phi)
            key.first.phi += IRStatement.Phi(result[key]!!, entry.map { (block, item) -> item to block })
        blocks.forEach { eliminateAlias(it) }
    }

    operator fun invoke(program: IRProgram, prefix: String) = try {
        program.function.forEach { this(it, prefix) }
    } catch (e: NullPointerException) {
        error("unexpected null pointer in mem2reg")
    }
}
