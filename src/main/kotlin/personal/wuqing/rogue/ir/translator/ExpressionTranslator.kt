package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ast.IdentifierReference
import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.grammar.operator.MxPrefix
import personal.wuqing.rogue.grammar.operator.MxSuffix
import personal.wuqing.rogue.grammar.operator.Operation
import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRCalcOp
import personal.wuqing.rogue.ir.grammar.IRCmpOp
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.map.FunctionMap
import personal.wuqing.rogue.ir.map.GlobalMap
import personal.wuqing.rogue.ir.map.LiteralMap
import personal.wuqing.rogue.ir.map.MemberMap

object ExpressionTranslator {
    private fun next() = IRItem.Local()

    sealed class ExprResult(val raw: IRItem) {
        abstract val value: IRItem

        class Value(override val value: IRItem) : ExprResult(value)
        class Address(private val addr: IRItem) : ExprResult(addr) {
            override val value
                get() = next().also {
                    statement(IRStatement.Normal.Load(dest = it, src = addr))
                }
        }
    }

    private val IRItem.asValue get() = ExprResult.Value(this)
    private val IRItem.asAddress get() = ExprResult.Address(this)

    operator fun invoke(ast: ASTNode.Expression): ExprResult = when (ast) {
        is ASTNode.Expression.NewObject -> this(ast)
        is ASTNode.Expression.NewArray -> this(ast)
        is ASTNode.Expression.MemberAccess -> this(ast)
        is ASTNode.Expression.MemberFunction -> this(ast)
        is ASTNode.Expression.Function -> this(ast)
        is ASTNode.Expression.Index -> this(ast)
        is ASTNode.Expression.Suffix -> this(ast)
        is ASTNode.Expression.Prefix -> this(ast)
        is ASTNode.Expression.Binary -> this(ast)
        is ASTNode.Expression.Ternary -> this(ast)
        is ASTNode.Expression.Identifier -> this(ast)
        is ASTNode.Expression.This -> thi?.asValue ?: error("unresolved this after semantic")
        is ASTNode.Expression.Constant.Int -> IRItem.Const(ast.value).asValue
        is ASTNode.Expression.Constant.String -> LiteralMap[ast.value].asValue
        is ASTNode.Expression.Constant.True -> IRItem.Const(1).asValue
        is ASTNode.Expression.Constant.False -> IRItem.Const(0).asValue
        is ASTNode.Expression.Constant.Null -> IRItem.Const(0).asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.NewObject): ExprResult {
        val mxType = ast.baseType.type as? MxType.Class ?: error("new non-class type found after semantic")
        val ret = next().also {
            statement(
                IRStatement.Normal.Call(it, IRFunction.Builtin.MallocObject, listOf(IRItem.Const(MemberMap[mxType])))
            )
        }
        for (variable in mxType.variables.values) if (variable.declaration.init != null) {
            val value = this(variable.declaration.init).value
            val member = next().also {
                statement(
                    IRStatement.Normal.ICalc(it, IRCalcOp.ADD, ret, IRItem.Const(MemberMap[mxType to variable] shl 2))
                )
            }
            statement(IRStatement.Normal.Store(src = value, dest = member))
        }
        val constructor = ast.baseType.type.functions["__constructor__"] ?: error("constructor not found")
        if (constructor !is MxFunction.Builtin.DefaultConstructor) {
            statement(IRStatement.Normal.Call(
                null, FunctionMap[constructor], listOf(ret) + ast.parameters.map { this(it).value }
            ))
        }
        return ret.asValue
    }

    private var arrayBlockCount = 0

    private fun arraySugar(length: List<IRItem>, parent: IRItem, current: Int) {
        if (current == length.size) return
        val id = arrayBlockCount++
        val cond = IRBlock("array.$id.cond")
        val body = IRBlock("array.$id.body")
        val end = IRBlock("array.$id.end")
        val total = next().also {
            statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, length[current - 1], IRItem.Const(1)))
        }
        val loop = next().also {
            statement(IRStatement.Normal.Alloca(it))
            statement(IRStatement.Normal.Store(src = total, dest = it))
        }
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(cond)
        val index = next().also { statement(IRStatement.Normal.Load(dest = it, src = loop)) }
        val delta = next().also { statement(IRStatement.Normal.ICalc(it, IRCalcOp.SHL, index, IRItem.Const(2))) }
        val location = next().also { statement(IRStatement.Normal.ICalc(it, IRCalcOp.ADD, parent, delta)) }
        val condition = next().also { statement(IRStatement.Normal.ICmp(it, IRCmpOp.SGE, index, IRItem.Const(0))) }
        statement(IRStatement.Terminate.Branch(cond = condition, then = body, els = end))
        enterNewBlock(body)
        val cur = next().also {
            statement(IRStatement.Normal.Call(it, IRFunction.Builtin.MallocArray, listOf(length[current])))
        }
        statement(IRStatement.Normal.Store(src = cur, dest = location))
        arraySugar(length, cur, current + 1)
        val next = next().also {
            statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, index, IRItem.Const(1)))
        }
        statement(IRStatement.Normal.Store(src = next, dest = loop))
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(end)
    }

    private operator fun invoke(ast: ASTNode.Expression.NewArray): ExprResult {
        val length = ast.length.map { this(it).value }
        val ret = next().also {
            statement(IRStatement.Normal.Call(it, IRFunction.Builtin.MallocArray, listOf(length[0])))
        }
        arraySugar(length, ret, 1)
        return ret.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberAccess): ExprResult {
        val parent = this(ast.parent).value
        val parentType = ast.parent.type as? MxType.Class ?: error("unexpected non-class")
        val variable = ast.reference
        return next().also {
            statement(
                IRStatement.Normal.ICalc(
                    it, IRCalcOp.ADD, parent, IRItem.Const(MemberMap[parentType to variable] shl 2)
                )
            )
        }.asAddress
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberFunction): ExprResult =
        when (ast.reference) {
            is MxFunction.Builtin.ArraySize -> {
                val address = next().also {
                    statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, this(ast.base).value, IRItem.Const(4)))
                }
                next().also {
                    statement(IRStatement.Normal.Load(dest = it, src = address))
                }.asValue
            }
            is MxFunction.Builtin.StringLength -> {
                next().also {
                    statement(IRStatement.Normal.Load(dest = it, src = this(ast.base).value))
                }.asValue
            }
            else -> {
                val parent = this(ast.base).value
                next().also {
                    statement(
                        IRStatement.Normal.Call(
                            result = it, function = FunctionMap[ast.reference],
                            args = listOf(parent) + ast.parameters.map { p -> this(p).value }
                        ))
                }.asValue
            }
        }

    private operator fun invoke(ast: ASTNode.Expression.Index): ExprResult {
        val parent = this(ast.parent).value
        val index = this(ast.child).value
        val delta = next().also {
            statement(IRStatement.Normal.ICalc(it, IRCalcOp.SHL, index, IRItem.Const(2)))
        }
        return next().also {
            statement(IRStatement.Normal.ICalc(it, IRCalcOp.ADD, parent, delta))
        }.asAddress
    }

    private operator fun invoke(ast: ASTNode.Expression.Function): ExprResult {
        val args = if (ast.reference.base == null)
            ast.parameters.map { this(it).value }
        else
            listOf(thi ?: error("this unresolved unexpectedly")) + ast.parameters.map { this(it).value }
        return next().also {
            statement(IRStatement.Normal.Call(it, FunctionMap[ast.reference], args))
        }.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.Suffix): ExprResult {
        val operand = this(ast.operand)
        val result = operand.value
        val after = next().also {
            statement(
                IRStatement.Normal.ICalc(
                    it, when (ast.operator) {
                        MxSuffix.INC -> IRCalcOp.ADD
                        MxSuffix.DEC -> IRCalcOp.SUB
                    }, result, IRItem.Const(1)
                )
            )
        }
        statement(IRStatement.Normal.Store(src = after, dest = operand.raw))
        return result.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.Prefix): ExprResult {
        val operand = this(ast.operand)
        val lvalue = operand.raw
        val rvalue = operand.value
        return when (ast.operator) {
            MxPrefix.INC, MxPrefix.DEC -> next().also {
                statement(
                    IRStatement.Normal.ICalc(
                        it, when (ast.operator) {
                            MxPrefix.INC -> IRCalcOp.ADD
                            MxPrefix.DEC -> IRCalcOp.SUB
                            else -> error("")
                        }, rvalue, IRItem.Const(1)
                    )
                )
                statement(IRStatement.Normal.Store(src = it, dest = lvalue))
            }
            MxPrefix.L_NEG -> next().also {
                statement(IRStatement.Normal.ICmp(it, IRCmpOp.EQ, rvalue, IRItem.Const(0)))
            }
            MxPrefix.INV -> next().also {
                statement(IRStatement.Normal.ICalc(it, IRCalcOp.XOR, rvalue, IRItem.Const(-1)))
            }
            MxPrefix.POS -> rvalue
            MxPrefix.NEG -> next().also {
                statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, IRItem.Const(0), rvalue))
            }
        }.asValue
    }

    private var shortCount = 0

    private operator fun invoke(ast: ASTNode.Expression.Binary): ExprResult {
        val operation = ast.operator.operation(ast.lhs.type, ast.rhs.type)
            ?: error("unknown operation of binary operator after semantic")
        val lhs = this(ast.lhs)
        val ll = lhs.raw
        val lr = lhs.value
        if (operation == Operation.BAnd || operation == Operation.BOr) {
            val current = block
            val id = shortCount++
            val second = IRBlock("short.$id.second")
            val result = IRBlock("short.$id.end")
            statement(
                if (operation == Operation.BAnd) IRStatement.Terminate.Branch(lr, second, result)
                else IRStatement.Terminate.Branch(lr, result, second)
            )
            enterNewBlock(second)
            val rr = this(ast.rhs).value
            statement(IRStatement.Terminate.Jump(result))
            enterNewBlock(result)
            val ret = next().also {
                statement(
                    IRStatement.Phi(
                        it, mapOf(current to IRItem.Const(if (operation == Operation.BAnd) 0 else 1), second to rr)
                    )
                )
            }
            return ret.asValue
        }
        val rhs = this(ast.rhs)
        val rr = rhs.value
        return when (operation) {
            Operation.BAssign, Operation.IAssign, Operation.SAssign -> ll.also {
                statement(IRStatement.Normal.Store(rr, ll))
            }
            Operation.BAnd, Operation.BOr -> error("should already handled")
            Operation.Plus, Operation.Minus, Operation.Times, Operation.Div, Operation.Rem,
            Operation.IAnd, Operation.IOr, Operation.Xor, Operation.Shl, Operation.Shr, Operation.UShr ->
                next().also {
                    statement(
                        IRStatement.Normal.ICalc(
                            it, when (operation) {
                                Operation.Plus -> IRCalcOp.ADD
                                Operation.Minus -> IRCalcOp.SUB
                                Operation.Times -> IRCalcOp.MUL
                                Operation.Div -> IRCalcOp.SDIV
                                Operation.Rem -> IRCalcOp.SREM
                                Operation.IAnd -> IRCalcOp.AND
                                Operation.IOr -> IRCalcOp.OR
                                Operation.Xor -> IRCalcOp.XOR
                                Operation.Shl -> IRCalcOp.SHL
                                Operation.Shr -> IRCalcOp.ASHR
                                Operation.UShr -> IRCalcOp.LSHR
                                else -> error("")
                            }, lr, rr
                        )
                    )
                }
            Operation.PlusI, Operation.MinusI, Operation.TimesI, Operation.DivI, Operation.RemI,
            Operation.AndI, Operation.OrI, Operation.XorI, Operation.ShlI, Operation.ShrI, Operation.UShrI ->
                next().also {
                    statement(
                        IRStatement.Normal.ICalc(
                            it, when (operation) {
                                Operation.PlusI -> IRCalcOp.ADD
                                Operation.MinusI -> IRCalcOp.SUB
                                Operation.TimesI -> IRCalcOp.MUL
                                Operation.DivI -> IRCalcOp.SDIV
                                Operation.RemI -> IRCalcOp.SREM
                                Operation.AndI -> IRCalcOp.AND
                                Operation.OrI -> IRCalcOp.OR
                                Operation.XorI -> IRCalcOp.XOR
                                Operation.ShlI -> IRCalcOp.SHL
                                Operation.ShrI -> IRCalcOp.ASHR
                                Operation.UShrI -> IRCalcOp.LSHR
                                else -> error("")
                            }, lr, rr
                        )
                    )
                    statement(IRStatement.Normal.Store(src = it, dest = ll))
                }
            Operation.Less, Operation.Leq, Operation.Greater, Operation.Geq, Operation.IEqual, Operation.INeq ->
                next().also {
                    statement(
                        IRStatement.Normal.ICmp(
                            it, when (operation) {
                                Operation.Less -> IRCmpOp.SLT
                                Operation.Leq -> IRCmpOp.SLE
                                Operation.Greater -> IRCmpOp.SGT
                                Operation.Geq -> IRCmpOp.SGE
                                Operation.IEqual -> IRCmpOp.EQ
                                Operation.INeq -> IRCmpOp.NE
                                else -> error("")
                            }, lr, rr
                        )
                    )
                }
            Operation.BEqual, Operation.BNeq -> next().also {
                statement(
                    IRStatement.Normal.ICmp(
                        it, when (operation) {
                            Operation.BEqual -> IRCmpOp.EQ
                            Operation.BNeq -> IRCmpOp.NE
                            else -> error("")
                        }, lr, rr
                    )
                )
            }
            Operation.SPlus -> next().also {
                statement(IRStatement.Normal.Call(it, IRFunction.Builtin.StringConcatenate, listOf(lr, rr)))
            }
            Operation.SPlusI -> next().also {
                statement(IRStatement.Normal.Call(it, IRFunction.Builtin.StringConcatenate, listOf(lr, rr)))
                statement(IRStatement.Normal.Store(src = it, dest = ll))
            }
            Operation.SLess, Operation.SLeq, Operation.SGreater, Operation.SGeq, Operation.SEqual, Operation.SNeq ->
                next().also {
                    val raw = next()
                    statement(
                        IRStatement.Normal.Call(
                            raw, when (operation) {
                                Operation.SLess -> IRFunction.Builtin.StringLess
                                Operation.SLeq -> IRFunction.Builtin.StringLeq
                                Operation.SGreater -> IRFunction.Builtin.StringGreater
                                Operation.SGeq -> IRFunction.Builtin.StringGeq
                                Operation.SEqual -> IRFunction.Builtin.StringEqual
                                Operation.SNeq -> IRFunction.Builtin.StringNeq
                                else -> error("")
                            }, listOf(lr, rr)
                        )
                    )
                    statement(IRStatement.Normal.ICmp(it, IRCmpOp.NE, raw, IRItem.Const(0)))
                }
            is Operation.PEqual -> next().also { statement(IRStatement.Normal.ICmp(it, IRCmpOp.EQ, lr, rr)) }
            is Operation.PNeq -> next().also { statement(IRStatement.Normal.ICmp(it, IRCmpOp.NE, lr, rr)) }
            is Operation.PAssign -> ll.also { statement(IRStatement.Normal.Store(src = rr, dest = ll)) }
        }.asValue
    }

    private var ternaryCount = 0

    private operator fun invoke(ast: ASTNode.Expression.Ternary): ExprResult {
        val cond = this(ast.condition).value
        val id = ternaryCount++
        val then = IRBlock(".ternary.$id.then")
        val els = IRBlock(".ternary.$id.else")
        val end = IRBlock(".ternary.$id.end")
        statement(IRStatement.Terminate.Branch(cond, then, els))
        enterNewBlock(then)
        val thenValue = this(ast.then).value
        statement(IRStatement.Terminate.Jump(end))
        enterNewBlock(els)
        val elsValue = this(ast.els).value
        statement(IRStatement.Terminate.Jump(end))
        enterNewBlock(end)
        return next().also {
            statement(IRStatement.Phi(it, listOf(then to thenValue, els to elsValue).toMap()))
        }.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.Identifier): ExprResult = when (val ref = ast.reference) {
        is IdentifierReference.Variable -> (local[ref.v] ?: GlobalMap[ref.v]).asAddress
        is IdentifierReference.Member ->
            next().also {
                statement(
                    IRStatement.Normal.ICalc(
                        it, IRCalcOp.ADD, thi ?: error("unresolved identifier after semantic"),
                        IRItem.Const(MemberMap[ref.parent to ref.v] shl 2)
                    )
                )
            }.asAddress
    }

    private operator fun invoke(ast: ASTNode.Expression.Constant.String): ExprResult {
        return LiteralMap[ast.value].asValue
    }
}