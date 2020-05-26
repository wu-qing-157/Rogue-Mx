package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

object GlobalLocalization {
    private val next = mutableMapOf<IRFunction.Declared, MutableSet<IRFunction.Declared>>()
    private val prev = mutableMapOf<IRFunction.Declared, MutableSet<IRFunction.Declared>>()
    private val use = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()
    private val def = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()
    private val defRec = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()
    private val useRec = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()

    private val IRFunction.Declared.next get() = GlobalLocalization.next.computeIfAbsent(this) { mutableSetOf() }
    private val IRFunction.Declared.prev get() = GlobalLocalization.prev.computeIfAbsent(this) { mutableSetOf() }
    private val IRFunction.Declared.use get() = GlobalLocalization.use.computeIfAbsent(this) { mutableSetOf() }
    private val IRFunction.Declared.def get() = GlobalLocalization.def.computeIfAbsent(this) { mutableSetOf() }
    private val IRFunction.Declared.defRec get() = GlobalLocalization.defRec.computeIfAbsent(this) { mutableSetOf() }
    private val IRFunction.Declared.useRec get() = GlobalLocalization.useRec.computeIfAbsent(this) { mutableSetOf() }

    private val useQueue = LinkedList<Pair<IRFunction.Declared, Iterable<IRItem.Global>>>()
    private val defQueue = LinkedList<Pair<IRFunction.Declared, Iterable<IRItem.Global>>>()

    private fun clear() {
        next.clear()
        prev.clear()
        use.clear()
        def.clear()
        defRec.clear()
        useRec.clear()
        useQueue.clear()
        defQueue.clear()
    }

    private fun analyzeUseDef(program: IRProgram) {
        for (func in program.function) {
            for (block in func.body) for (st in block.normal) {
                if (st is IRStatement.Normal.Call && st.function is IRFunction.Declared) {
                    func.next += st.function
                    st.function.prev += func
                }
                if (st is IRStatement.Normal.Store && st.dest is IRItem.Global) func.def += st.dest
                if (st is IRStatement.Normal.Load && st.src is IRItem.Global) func.use += st.src
            }
            func.defRec += func.def
            func.useRec += func.use
            defQueue += func to func.def.toSet()
            useQueue += func to func.use.toSet()
        }
        while (defQueue.isNotEmpty()) defQueue.poll()?.let { (f, l) ->
            f.prev.forEach {
                val delta = it.defRec - l
                if (delta.isNotEmpty()) {
                    it.defRec += l
                    defQueue += it to delta
                }
            }
        }
        while (useQueue.isNotEmpty()) useQueue.poll()?.let { (f, l) ->
            f.prev.forEach {
                val delta = it.useRec - l
                if (delta.isNotEmpty()) {
                    it.useRec += l
                    useQueue += it to delta
                }
            }
        }
    }

    private fun localize(func: IRFunction.Declared) {
        val items = (func.use + func.def).associateWith { IRItem.Local() }
        for (block in func.body) {
            val newNormal = mutableListOf<IRStatement.Normal>()
            for (st in block.normal) when (st) {
                is IRStatement.Normal.Call -> if (st.function is IRFunction.Declared) {
                    st.function.useRec.filter { it in func.def }.forEach {
                        val local = IRItem.Local()
                        newNormal += IRStatement.Normal.Load(local, items[it] ?: error("cannot find global"))
                        newNormal += IRStatement.Normal.Store(local, it)
                    }
                    newNormal += st
                    st.function.defRec.filter { it in func.use }.forEach {
                        val local = IRItem.Local()
                        newNormal += IRStatement.Normal.Load(local, it)
                        newNormal += IRStatement.Normal.Store(local, items[it] ?: error("cannot find global"))
                    }
                } else newNormal += st
                is IRStatement.Normal.Load ->
                    if (st.src is IRItem.Global)
                        newNormal += IRStatement.Normal.Load(st.dest, items[st.src] ?: error("cannot find global"))
                    else newNormal += st
                is IRStatement.Normal.Store ->
                    if (st.dest is IRItem.Global)
                        newNormal += IRStatement.Normal.Store(st.src, items[st.dest] ?: error("cannot find global"))
                    else newNormal += st
                else -> newNormal += st
            }
            if (block.terminate is IRStatement.Terminate.Ret) items.forEach { (k, v) ->
                if (k in func.def) {
                    val local = IRItem.Local()
                    newNormal += IRStatement.Normal.Load(local, v)
                    newNormal += IRStatement.Normal.Store(local, k)
                }
            }
            block.normal.clear()
            block.normal += newNormal
        }
        val entry = mutableListOf<IRStatement.Normal>().apply {
            items.forEach { (k, v) ->
                val local = IRItem.Local()
                add(IRStatement.Normal.Alloca(v))
                add(IRStatement.Normal.Load(local, k))
                add(IRStatement.Normal.Store(local, v))
            }
        }
        func.body[0].normal.addAll(0, entry)
    }

    operator fun invoke(program: IRProgram) {
        clear()
        analyzeUseDef(program)
        program.function.forEach(::localize)
    }
}
