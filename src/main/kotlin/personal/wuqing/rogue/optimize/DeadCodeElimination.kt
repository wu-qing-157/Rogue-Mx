package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

object DeadCodeElimination {
    private fun analyze(function: IRFunction.Declared, analysis: FunctionCallAnalysis) {
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
                if ((it is IRStatement.Normal.Call && analysis.sideEffect(it.function)) ||
                    it is IRStatement.Normal.Store
                ) live += it
            }
            live += block.terminate
        }
        val used = live.map { it.use }.flatten().toMutableSet()
        val useQueue = LinkedList(used)
        while (useQueue.isNotEmpty()) useQueue.poll()?.let { def[it] }?.let {
            live += it
            useQueue += it.use - used
            used += it.use
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
        val analysis = FunctionCallAnalysis(program)
        program.function.forEach { analyze(it, analysis) }
    }
}
