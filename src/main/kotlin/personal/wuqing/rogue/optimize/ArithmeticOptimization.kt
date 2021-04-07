package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement

object ArithmeticOptimization {
    private val powOf2 = (0..30).map { 1 shl it }
    private fun Int.powOf2() = powOf2.binarySearch(this).takeIf { it >= 0 }

    operator fun invoke(program: IRProgram) {
        for (func in program.function) {
            for (block in func.body) block.normal.replaceAll {
                if (it is IRStatement.Normal.ICalc) when (it.operator) {
                    IRCalcOp.ADD -> if (it.op1 is IRItem.Const && it.op2 !is IRItem.Const) {
                        IRStatement.Normal.ICalc(it.result, IRCalcOp.ADD, it.op2, it.op1)
                    } else it
                    IRCalcOp.SUB -> if (it.op2 is IRItem.Const) {
                        IRStatement.Normal.ICalc(it.result, IRCalcOp.ADD, it.op1, IRItem.Const(-it.op2.value))
                    } else it
                    IRCalcOp.MUL -> when {
                        it.op1 == IRItem.Const(0) || it.op2 == IRItem.Const(0) ->
                            IRStatement.Normal.ICalc(it.result, IRCalcOp.ADD, IRItem.Const(0), IRItem.Const(0))
                        it.op2 is IRItem.Const -> it.op2.value.powOf2()?.let { delta ->
                            IRStatement.Normal.ICalc(it.result, IRCalcOp.SHL, it.op1, IRItem.Const(delta))
                        } ?: it
                        it.op1 is IRItem.Const -> it.op1.value.powOf2()?.let { delta ->
                            IRStatement.Normal.ICalc(it.result, IRCalcOp.SHL, it.op2, IRItem.Const(delta))
                        } ?: it
                        else -> it
                    }
                    IRCalcOp.SDIV -> if (it.op2 is IRItem.Const) it.op2.value.powOf2()?.let { delta ->
                        IRStatement.Normal.ICalc(it.result, IRCalcOp.ASHR, it.op1, IRItem.Const(delta))
                    } ?: it else it
                    else -> it
                } else it
            }
            val addMap = mutableMapOf<IRItem.Local, Pair<IRItem.Local, Int>>()
            val mulMap = mutableMapOf<IRItem.Local, Pair<IRItem.Local, Int>>()
            for (block in func.body) block.normal.filterIsInstance<IRStatement.Normal.ICalc>().forEach {
                if (it.op1 is IRItem.Local && it.op2 is IRItem.Const) when (it.operator) {
                    IRCalcOp.ADD -> addMap[it.result] = it.op1 to it.op2.value
                    IRCalcOp.MUL -> mulMap[it.result] = it.op1 to it.op2.value
                    IRCalcOp.SHL -> mulMap[it.result] = it.op1 to powOf2[it.op2.value]
                    else -> Unit
                }
            }
            val addFinalMap = addMap.keys.associateWith {
                var ret = it
                var add = 0
                while (true) {
                    val (r, a) = addMap[ret] ?: break
                    ret = r
                    add += a
                }
                ret to add
            }
            val mulFinalMap = addMap.keys.associateWith {
                var ret = it
                var mul = 1
                while (true) {
                    val (r, m) = addMap[ret] ?: break
                    ret = r
                    mul *= m
                }
                ret to mul
            }
            val trans = mutableMapOf<IRItem, IRItem>()
            for (block in func.body) block.normal.replaceAll {
                if (it is IRStatement.Normal.ICalc)
                    addFinalMap[it.result]?.let { (r, a) ->
                        if (a == 0) trans[it.result] = r
                        IRStatement.Normal.ICalc(it.result, IRCalcOp.ADD, r, IRItem.Const(a))
                    } ?: mulFinalMap[it.result]?.let { (r, m) ->
                        if (m == 1) trans[it.result] = r
                        m.powOf2()?.let { p -> IRStatement.Normal.ICalc(it.result, IRCalcOp.SHL, r, IRItem.Const(p)) }
                            ?: IRStatement.Normal.ICalc(it.result, IRCalcOp.MUL, r, IRItem.Const(m))
                    } ?: it
                else it
            }
            val finalTrans = trans.keys.associateWith {
                var ret = it
                while (true) ret = trans[ret] ?: break
                ret
            }
            for (block in func.body) {
                block.phi.replaceAll { it.transUse(finalTrans) }
                block.normal.replaceAll { it.transUse(finalTrans) }
                block.terminate = block.terminate.transUse(finalTrans)
            }
        }
    }
}
