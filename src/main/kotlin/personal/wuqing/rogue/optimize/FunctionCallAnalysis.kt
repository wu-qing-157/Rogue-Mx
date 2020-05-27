package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

class FunctionCallAnalysis private constructor(builder: Builder) {
    constructor(program: IRProgram, andersen: Andersen? = null) : this(Builder(program, andersen))

    val sideEffect: Set<IRFunction> = builder.sideEffect
    private val stored: Map<IRFunction, Set<IRItem>> = builder.stored

    operator fun get(func: IRFunction) = stored[func] ?: setOf()

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

        init {
            program.function.forEach(::analyzeCaller)
            analyzeSideEffect(program)
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
