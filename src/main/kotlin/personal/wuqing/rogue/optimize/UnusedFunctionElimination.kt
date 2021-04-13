package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

object UnusedFunctionElimination {
    operator fun invoke(program: IRProgram) {
        val main = program.function.single { it.name == "main" }
        val visited = mutableSetOf(main)
        val queue = LinkedList(listOf(main))
        while (true) {
            val cur = queue.poll() ?: break
            for (block in cur.body)
                block.normal.filterIsInstance<IRStatement.Normal.Call>().map { it.function }
                    .filterIsInstance<IRFunction.Declared>().filter { it !in visited }.let {
                    visited += it
                    queue += it
                }
        }
        program.function -= program.function - visited
    }
}
