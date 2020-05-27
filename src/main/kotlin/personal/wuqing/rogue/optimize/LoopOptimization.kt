package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.utils.DomTree
import java.util.Comparator
import java.util.LinkedList

object LoopOptimization {
    private class Loop(val head: IRBlock, val component: MutableSet<IRBlock>) {
        override fun toString() = head.toString()
    }

    private val loops = mutableMapOf<IRBlock, Loop>()
    private val children = mutableMapOf<Loop, Set<Loop>>()
    private val prev = mutableMapOf<Loop, IRBlock>()
    private val added = mutableMapOf<IRBlock, MutableList<IRBlock>>()
    private val Loop.children get() = LoopOptimization.children[this] ?: setOf()
    private val Loop.prev get() = LoopOptimization.prev[this] ?: error("cannot find previous-header")
    private val IRBlock.added get() = LoopOptimization.added.computeIfAbsent(this) { mutableListOf() }

    private val dom = mutableMapOf<IRBlock, Set<IRBlock>>()
    private val IRBlock.dom get() = LoopOptimization.dom[this] ?: setOf()

    private val related = mutableMapOf<IRItem, MutableSet<IRStatement.Normal>>()
    private val IRItem.related get() = LoopOptimization.related.computeIfAbsent(this) { mutableSetOf() }
    private val queue = LinkedList<IRItem>()

    private var count = 0

    private fun clear() {
        loops.clear()
        children.clear()
        prev.clear()
        added.clear()
        dom.clear()
    }

    private fun calcDom(func: IRFunction.Declared) {
        func.updatePrev()
        val tree = DomTree(func.body[0])
        fun visit(cur: IRBlock) {
            for (child in tree.child[cur] ?: listOf()) {
                dom[child] = cur.dom + cur
                visit(child)
            }
        }
        visit(func.body[0])
    }

    private fun <T> disjoint(a: Set<T>, b: Set<T>) = a.none { it in b } && b.none { it in a }

    private fun invariant(loop: Loop, andersen: Andersen, analysis: FunctionCallAnalysis) {
        val stored = loop.component.map { b ->
            b.normal.filterIsInstance<IRStatement.Normal.Store>().map { andersen[it.dest] }.flatten() +
                    b.normal.filterIsInstance<IRStatement.Normal.Call>().map { analysis.stored(it.function) }.flatten()
        }.flatten().toSet()
        related.clear()
        queue.clear()
        val inner = loop.component.map { b ->
            (b.phi.map { it.result } + b.normal.filterIsInstance<IRStatement.WithResult>().mapNotNull { it.result })
        }.flatten().toMutableSet()
        val toDelete = mutableSetOf<IRStatement.Normal>()
        for (block in loop.component) {
            for (st in block.normal) {
                if (st is IRStatement.Normal.ICalc)
                    for (use in st.use) {
                        use.related += st
                        if (use !is IRItem.Local || use !in inner) queue += use
                    }
                if (st is IRStatement.Normal.Load) if (disjoint(andersen[st.src], stored)) {
                    st.src.related += st
                    if (st.src !is IRItem.Local || st.src !in inner) queue += st.src
                }
            }
        }
        while (queue.isNotEmpty()) queue.poll()?.let {
            it.related.forEach { st ->
                if (st !in toDelete && st.use.all { use -> use !is IRItem.Local || use !in inner }) {
                    loop.prev.normal += st
                    toDelete += st
                    if (st is IRStatement.WithResult) st.result?.let { result ->
                        inner -= result
                        queue += result
                    }
                }
            }
        }
        loop.component.forEach { it.normal.removeIf { st -> st in toDelete } }
    }

    private fun process(func: IRFunction.Declared, andersen: Andersen, analysis: FunctionCallAnalysis) {
        clear()
        calcDom(func)
        for (block in func.body) for (next in block.next) if (next in block.dom) {
            assert(next !in loops)
            val visited = mutableSetOf(next)
            val component = mutableSetOf(next)
            fun visit(cur: IRBlock) {
                if (cur in visited) return
                visited += cur
                if (next in cur.dom) component += cur
                for (n in cur.prev) visit(n)
            }
            visit(block)
            loops.computeIfAbsent(next) { Loop(next, mutableSetOf()) }.component += component
        }
        children +=
            loops.values.associateWith { k -> loops.values.filter { it != k && it.head in k.component }.toSet() }
        loops.values.sortedWith(Comparator.comparingInt { it.component.size }).forEach {
            it.component += it.children.map { c -> c.prev }
            val prev = IRBlock("prev.${count++}")
            val outer = it.head.prev - it.component
            val newPhiResult = it.head.phi.associateWith { IRItem.Local() }
            prev.phi += it.head.phi.map { st ->
                IRStatement.Phi(newPhiResult[st] ?: error("cannot find phi"), st.list - it.component)
            }
            it.head.phi.replaceAll { st ->
                IRStatement.Phi(st.result, st.list - outer + (prev to (newPhiResult[st] ?: error("cannot find phi"))))
            }
            prev.terminate = IRStatement.Terminate.Jump(it.head)
            outer.forEach { o -> o.terminate = o.terminate.translate(mapOf(it.head to prev)) }
            prev.prev += outer
            it.head.prev -= outer
            it.head.prev += prev
            this.prev[it] = prev
            it.head.added += prev
        }
        val newBody = mutableListOf<IRBlock>()
        func.body.forEach {
            newBody += it.added
            newBody += it
        }
        func.body.clear()
        func.body += newBody
        loops.values.forEach { invariant(it, andersen, analysis) }
    }

    operator fun invoke(program: IRProgram) {
        val andersen = Andersen(program)
        val analysis = FunctionCallAnalysis(program, andersen)
        program.function.forEach { process(it, andersen, analysis) }
    }
}
