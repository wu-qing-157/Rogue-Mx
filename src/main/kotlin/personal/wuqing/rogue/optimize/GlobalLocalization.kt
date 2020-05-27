package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement

object GlobalLocalization {
    private fun localize(func: IRFunction.Declared, analysis: FunctionCallAnalysis) {
        val items = (analysis.use(func) + analysis.def(func)).associateWith { IRItem.Local() }
        for (block in func.body) {
            val newNormal = mutableListOf<IRStatement.Normal>()
            for (st in block.normal) when (st) {
                is IRStatement.Normal.Call -> if (st.function is IRFunction.Declared) {
                    analysis.useRec(st.function).filter { it in items }.forEach {
                        val local = IRItem.Local()
                        newNormal += IRStatement.Normal.Load(local, items[it] ?: error("cannot find global"))
                        newNormal += IRStatement.Normal.Store(local, it)
                    }
                    newNormal += st
                    analysis.defRec(st.function).filter { it in items }.forEach {
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
                if (k in items) {
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
        val analysis = FunctionCallAnalysis(program)
        program.function.forEach { localize(it, analysis) }
    }
}
