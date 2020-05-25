package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement

object FunctionElimination {
    operator fun invoke(program: IRProgram) {
        val visited = mutableSetOf<IRFunction.Declared>()
        fun visit(func: IRFunction.Declared) {
            visited += func
            for (block in func.body) for (st in block.normal)
                if (st is IRStatement.Normal.Call && st.function is IRFunction.Declared && st.function !in visited)
                    visit(st.function)
        }
        visit(program.main)
        program.function.removeAll { it !in visited }
    }
}
