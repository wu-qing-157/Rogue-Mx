package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

object DeadCodeElimination {
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

    private fun analyze(function: IRFunction.Declared) {
        val def = mutableMapOf<IRItem.Local, IRStatement>()
        for (block in function.body) {
            block.phi.forEach { def[it.result] = it }
            block.normal.forEach {
                when (it) {
                    is IRStatement.Normal.Load -> def[it.dest] = it
                    is IRStatement.Normal.Store -> Unit
                    is IRStatement.Normal.Alloca -> error("alloca in DCE")
                    is IRStatement.Normal.ICalc -> def[it.result] = it
                    is IRStatement.Normal.ICmp -> def[it.result] = it
                    is IRStatement.Normal.Call -> if (it.result != null) def[it.result] = it
                    IRStatement.Normal.NOP -> error("nop in DEC")
                }
            }
        }
        val live = mutableSetOf<IRStatement>()
        for (block in function.body) {
            block.normal.forEach {
                if ((it is IRStatement.Normal.Call && it.function in sideEffect) || it is IRStatement.Normal.Store)
                    live += it
            }
            live += block.terminate
        }
        val used = live.map { it.using }.flatten().toMutableSet()
        val useQueue = LinkedList(used)
        while (useQueue.isNotEmpty()) useQueue.poll()?.let { def[it] }?.let {
            live += it
            useQueue += it.using - used
            used += it.using
        }
        for (block in function.body) {
            block.phi.removeAll { it !in live }
            block.normal.removeAll { it !in live }
            block.normal.replaceAll {
                if (it is IRStatement.Normal.Call && it.result != null && it.result !in used)
                    IRStatement.Normal.Call(null, it.function, it.args)
                else it
            }
        }
    }

    operator fun invoke(program: IRProgram) {
        program.function.forEach(::analyzeCaller)
        analyzeSideEffect(program)
        program.function.forEach(::analyze)
    }
}
