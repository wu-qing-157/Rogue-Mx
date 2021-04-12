package personal.wuqing.rogue.optimize

import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRCmpOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import java.util.LinkedList

object ConstantPropagation {
    private val constant = mutableMapOf<IRItem, IRItem>()
    private val count = mutableMapOf<IRStatement.WithResult, Int>()
    private val related = mutableMapOf<IRItem, MutableList<IRStatement.WithResult>>()
    private val queue = LinkedList<IRItem>()

    private val IRItem.value
        get() = when (this) {
            is IRItem.Const -> value
            in constant -> (constant[this] as? IRItem.Const ?: throw NonDeterminedException).value
            else -> throw NonDeterminedException
        }

    private val IRItem.literal
        get() = when (this) {
            is IRItem.Literal -> value
            in constant -> (constant[this] as? IRItem.Literal ?: throw NonDeterminedException).value
            else -> throw NonDeterminedException
        }

    object NonDeterminedException : Exception()

    private fun check(item: IRItem) {
        for (func in related[item] ?: mutableListOf())
            (count[func]!! - 1).also { count[func] = it }.takeIf { it == 0 }?.let { check(func) }
    }

    private fun check(st: IRStatement.WithResult) {
        if (st.result as IRItem? in constant) return

        try {
            when (st) {
                is IRStatement.Normal.ICalc -> {
                    val op1 = st.op1.value
                    val op2 = st.op2.value
                    val result = when (st.operator) {
                        IRCalcOp.ADD -> op1 + op2
                        IRCalcOp.SUB -> op1 - op2
                        IRCalcOp.MUL -> op1 * op2
                        IRCalcOp.SDIV -> op1 / op2
                        IRCalcOp.SREM -> op1 % op2
                        IRCalcOp.AND -> op1 and op2
                        IRCalcOp.OR -> op1 or op2
                        IRCalcOp.XOR -> op1 xor op2
                        IRCalcOp.SHL -> op1 shl op2
                        IRCalcOp.ASHR -> op1 shr op2
                        IRCalcOp.LSHR -> op1 ushr op2
                    }
                    constant[st.result] = IRItem.Const(result)
                    queue += st.result
                }
                is IRStatement.Normal.ICmp -> {
                    val op1 = st.op1.value
                    val op2 = st.op2.value
                    val result = when (st.operator) {
                        IRCmpOp.EQ -> op1 == op2
                        IRCmpOp.NE -> op1 != op2
                        IRCmpOp.SLT -> op1 < op2
                        IRCmpOp.SLE -> op1 <= op2
                        IRCmpOp.SGT -> op1 > op2
                        IRCmpOp.SGE -> op1 >= op2
                    }
                    constant[st.result] = IRItem.Const(if (result) 1 else 0)
                    queue += st.result
                }
                is IRStatement.Normal.Call -> {
                    when (st.function) {
                        IRFunction.Builtin.ToString -> {
                            constant[st.result!!] = IRItem.Literal(st.args[0].value.toString())
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringParse -> {
                            constant[st.result!!] = IRItem.Const(st.args[0].literal.toInt())
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringOrd -> {
                            constant[st.result!!] = IRItem.Const(st.args[0].literal[st.args[1].value].toInt())
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringSubstring -> {
                            constant[st.result!!] =
                                IRItem.Literal(st.args[0].literal.substring(st.args[1].value, st.args[2].value))
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringConcatenate -> {
                            constant[st.result!!] = IRItem.Literal(st.args[0].literal + st.args[1].literal)
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringEqual -> {
                            val result = st.args[0].literal == st.args[1].literal
                            constant[st.result!!] = IRItem.Const(if (result) 1 else 0)
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringNeq -> {
                            val result = st.args[0].literal != st.args[1].literal
                            constant[st.result!!] = IRItem.Const(if (result) 1 else 0)
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringLess -> {
                            val result = st.args[0].literal < st.args[1].literal
                            constant[st.result!!] = IRItem.Const(if (result) 1 else 0)
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringLeq -> {
                            val result = st.args[0].literal <= st.args[1].literal
                            constant[st.result!!] = IRItem.Const(if (result) 1 else 0)
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringGreater -> {
                            val result = st.args[0].literal > st.args[1].literal
                            constant[st.result!!] = IRItem.Const(if (result) 1 else 0)
                            queue += st.result!!
                        }
                        IRFunction.Builtin.StringGeq -> {
                            val result = st.args[0].literal >= st.args[1].literal
                            constant[st.result!!] = IRItem.Const(if (result) 1 else 0)
                            queue += st.result!!
                        }
                        else -> throw NonDeterminedException
                    }
                    queue += st.result!!
                }
                is IRStatement.Normal.Load -> {
                    constant[st.dest] = IRItem.Const(st.src.literal.length)
                    queue += st.result
                }
                is IRStatement.Phi -> {
                    st.list.values.first().let { constant[it] ?: it }.takeIf {
                        (it is IRItem.Literal && st.list.values.all { v -> v.literal == it.literal })
                                || (it is IRItem.Const && st.list.values.all { v -> v.value == it.value })
                    }?.let { constant[st.result] = it }
                    queue += st.result
                }
            }
        } catch (e: ArithmeticException) {
        } catch (e: NumberFormatException) {
        } catch (e: IndexOutOfBoundsException) {
        } catch (e: NonDeterminedException) {
        }
    }

    operator fun invoke(program: IRProgram) {
        val initialQueue = mutableSetOf<IRItem>()
        for (func in program.function) for (block in func.body) {
            block.phi.forEach { st ->
                st.use.forEach {
                    related.computeIfAbsent(it) { mutableListOf() } += st
                    count[st] = (count[st] ?: 0) + 1
                    if (it is IRItem.Literal || it is IRItem.Const) initialQueue += it
                }
            }
            block.normal.forEach { st ->
                if (st is IRStatement.WithResult && (st !is IRStatement.Normal.Call || st.function is IRFunction.Builtin || st.result != null)) st.use.forEach {
                    related.computeIfAbsent(it) { mutableListOf() } += st as IRStatement.WithResult
                    count[st] = (count[st] ?: 0) + 1
                    if (it is IRItem.Literal || it is IRItem.Const) initialQueue += it
                }
            }
        }
        queue += initialQueue
        while (queue.isNotEmpty()) queue.poll()?.let { check(it) }
        program.literal += constant.values.filterIsInstance<IRItem.Literal>()
        for (func in program.function) for (block in func.body) {
            block.phi.replaceAll { it.transUse(constant) }
            block.normal.replaceAll { it.transUse(constant) }
            block.terminate = block.terminate.transUse(constant)
        }
    }
}
