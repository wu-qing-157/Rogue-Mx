package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement

object ArithmeticOptimization {
    private val powOf2 = (0..30).map { 1 shl it }
    private fun Int.powOf2() = powOf2.binarySearch(this).takeIf { it >= 0 }

    operator fun invoke(program: IRProgram) {
        for (func in program.function) for (block in func.body) block.normal.replaceAll {
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
    }
}
