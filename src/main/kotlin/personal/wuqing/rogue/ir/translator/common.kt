package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRStatement

fun statement(statement: IRStatement.Phi) = TopLevelTranslator.statement(statement)
fun statement(statement: IRStatement.Normal) = TopLevelTranslator.statement(statement)
fun statement(statement: IRStatement.Terminate) = TopLevelTranslator.statement(statement)
fun enterNewBlock(block: IRBlock) = TopLevelTranslator.enterNewBlock(block)
val loopTarget get() = TopLevelTranslator.loopTarget
val thi get() = TopLevelTranslator.thi
val terminating get() = TopLevelTranslator.terminating
val local get() = TopLevelTranslator.local
val block get() = TopLevelTranslator.block
