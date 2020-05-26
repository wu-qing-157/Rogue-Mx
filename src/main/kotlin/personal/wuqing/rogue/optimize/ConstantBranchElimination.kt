package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement

object ConstantBranchElimination {
    operator fun invoke(program: IRProgram) {
        for (func in program.function) for (block in func.body)
            (block.terminate as? IRStatement.Terminate.Branch)?.let {
                when (it.cond) {
                    IRItem.Const(0) -> {
                        it.then.phi.replaceAll { st -> IRStatement.Phi(st.result, st.list - block) }
                        block.terminate = IRStatement.Terminate.Jump(it.els)
                    }
                    IRItem.Const(1) -> {
                        it.els.phi.replaceAll { st -> IRStatement.Phi(st.result, st.list - block) }
                        block.terminate = IRStatement.Terminate.Jump(it.then)
                    }
                    else -> Unit
                }
            }
    }
}
