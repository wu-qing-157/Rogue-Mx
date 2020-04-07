package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.grammar.IRType

fun IRItem.nullable(expect: IRType) = if (this is IRItem.Null) IRItem.Null(expect) else this
fun statement(statement: IRStatement.Phi) = TopLevelTranslator.statement(statement)
fun statement(statement: IRStatement.Normal) = TopLevelTranslator.statement(statement)
fun statement(statement: IRStatement.Terminate) = TopLevelTranslator.statement(statement)
fun enterNewBlock(block: IRBlock) = TopLevelTranslator.enterNewBlock(block)
val loopTarget get() = TopLevelTranslator.loopTarget
val thi get() = TopLevelTranslator.thi
val returnType get() = TopLevelTranslator.returnType
val terminating get() = TopLevelTranslator.terminating
fun next(type: IRType) = TopLevelTranslator.next(type)
val local get() = TopLevelTranslator.local
val block get() = TopLevelTranslator.block
