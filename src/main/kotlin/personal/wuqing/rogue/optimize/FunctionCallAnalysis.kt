package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

class FunctionCallAnalysis private constructor(builder: Builder) {
    constructor(program: IRProgram, andersen: Andersen? = null) : this(Builder(program, andersen))

    private val sideEffect: Set<IRFunction> = builder.sideEffect
    private val stored: Map<IRFunction, Set<IRItem>> = builder.stored
    private val use: Map<IRFunction.Declared, Set<IRItem.Global>> = builder.use
    private val def: Map<IRFunction.Declared, Set<IRItem.Global>> = builder.def
    private val useRec: Map<IRFunction.Declared, Set<IRItem.Global>> = builder.useRec
    private val defRec: Map<IRFunction.Declared, Set<IRItem.Global>> = builder.defRec

    fun sideEffect(func: IRFunction) = func in sideEffect
    fun stored(func: IRFunction) = stored[func] ?: setOf()
    fun use(func: IRFunction) = use[func] ?: setOf()
    fun def(func: IRFunction) = def[func] ?: setOf()
    fun useRec(func: IRFunction) = useRec[func] ?: setOf()
    fun defRec(func: IRFunction) = defRec[func] ?: setOf()

    class Builder(program: IRProgram, andersen: Andersen? = null) {
        private val callerMap = mutableMapOf<IRFunction, MutableList<IRFunction>>()

        val sideEffect = mutableSetOf<IRFunction>()
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

        val stored = mutableMapOf<IRFunction, MutableSet<IRItem>>()
        private val IRFunction.stored get() = this@Builder.stored.computeIfAbsent(this) { mutableSetOf() }
        private val storedQueue = LinkedList<IRFunction>()

        private val next = mutableMapOf<IRFunction.Declared, MutableSet<IRFunction.Declared>>()
        private val prev = mutableMapOf<IRFunction.Declared, MutableSet<IRFunction.Declared>>()
        val use = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()
        val def = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()
        val defRec = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()
        val useRec = mutableMapOf<IRFunction.Declared, MutableSet<IRItem.Global>>()

        private val IRFunction.Declared.next get() = this@Builder.next.computeIfAbsent(this) { mutableSetOf() }
        private val IRFunction.Declared.prev get() = this@Builder.prev.computeIfAbsent(this) { mutableSetOf() }
        private val IRFunction.Declared.use get() = this@Builder.use.computeIfAbsent(this) { mutableSetOf() }
        private val IRFunction.Declared.def get() = this@Builder.def.computeIfAbsent(this) { mutableSetOf() }
        private val IRFunction.Declared.defRec get() = this@Builder.defRec.computeIfAbsent(this) { mutableSetOf() }
        private val IRFunction.Declared.useRec get() = this@Builder.useRec.computeIfAbsent(this) { mutableSetOf() }

        private val useQueue = LinkedList<IRFunction.Declared>()
        private val defQueue = LinkedList<IRFunction.Declared>()

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
            }
            defQueue += program.function
            useQueue += program.function
            while (defQueue.isNotEmpty()) defQueue.poll()?.let { n ->
                n.prev.forEach { p ->
                    if (n.defRec.any { it !in p.defRec }) {
                        p.defRec += n.defRec
                        defQueue += p
                    }
                }
            }
            while (useQueue.isNotEmpty()) useQueue.poll()?.let { n ->
                n.prev.forEach { p ->
                    if (n.useRec.any { it !in p.useRec }) {
                        p.useRec += n.useRec
                        useQueue += p
                    }
                }
            }
        }

        init {
            program.function.forEach(::analyzeCaller)
            analyzeSideEffect(program)
            analyzeUseDef(program)
            if (andersen != null) {
                for (f in program.function) for (b in f.body) for (st in b.normal)
                    if (st is IRStatement.Normal.Store) f.stored += andersen[st.dest]
                storedQueue += program.function
                while (storedQueue.isNotEmpty()) storedQueue.poll()?.let { f ->
                    for (caller in callerMap[f] ?: mutableListOf()) if (f.stored.any { it !in caller.stored }) {
                        caller.stored += f.stored
                        storedQueue += caller
                    }
                }
            }
        }
    }
}
