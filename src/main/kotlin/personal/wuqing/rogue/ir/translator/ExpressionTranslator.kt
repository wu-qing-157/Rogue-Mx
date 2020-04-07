package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ast.ReferenceType
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
import personal.wuqing.rogue.ir.grammar.IRType
import personal.wuqing.rogue.ir.map.FunctionMap
import personal.wuqing.rogue.ir.map.GlobalMap
import personal.wuqing.rogue.ir.map.LiteralMap
import personal.wuqing.rogue.ir.map.TypeMap

object ExpressionTranslator {
    sealed class ExprResult(val raw: IRItem) {
        abstract val value: IRItem

        class Value(override val value: IRItem) : ExprResult(value)
        class Address(private val addr: IRItem) : ExprResult(addr) {
            override val value
                get() = next((addr.type as IRType.Address).type).also {
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
        is ASTNode.Expression.Constant.Int -> (IRType.I32 const ast.value).asValue
        is ASTNode.Expression.Constant.String -> this(ast)
        is ASTNode.Expression.Constant.True -> (IRType.I1 const 1).asValue
        is ASTNode.Expression.Constant.False -> (IRType.I1 const 0).asValue
        is ASTNode.Expression.Constant.Null -> IRItem.Null(IRType.Null).asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.NewObject): ExprResult {
        val mxType = ast.baseType.type as? MxType.Class ?: error("new non-class type found after semantic")
        val irType = TypeMap[mxType] as? IRType.Class ?: error("invalid class type status")
        val ret = next(irType).also {
            statement(IRStatement.Normal.MallocObject(it))
        }
        for (variable in irType.members.members) if (variable.declaration.init != null) {
            val value = this(variable.declaration.init).value
            val memberType = TypeMap[variable.type]
            val member = next(memberType).also { statement(IRStatement.Normal.Member(it, ret, variable)) }
            statement(IRStatement.Normal.Store(src = value, dest = member))
        }
        val constructor =
            ast.baseType.type.functions["__constructor__"] ?: error("constructor not found after semantic")
        if (constructor !is MxFunction.Builtin.DefaultConstructor) {
            statement(
                IRStatement.Normal.Call(
                    result = next(IRType.Void),
                    function = FunctionMap[constructor],
                    args = listOf(ret) + FunctionMap[constructor].args.run { subList(1, size) }.zip(ast.parameters)
                        .map { (t, a) -> this(a).value.nullable(t) }
                ))
        }
        return ret.asValue
    }

    private var arrayBlockCount = 0

    private fun arraySugar(length: List<IRItem>, parent: IRItem, current: Int) {
        if (current == length.size) return
        val type = (parent.type as? IRType.Array)?.base ?: error("unexpected non-array type")
        val id = arrayBlockCount++
        val cond = IRBlock("array.$id.cond")
        val body = IRBlock("array.$id.body")
        val end = IRBlock("array.$id.end")
        val total = next(IRType.I32).also {
            statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, length[current - 1], IRType.I32 const 0))
        }
        val loop = next(IRType.Address(IRType.I32)).also {
            statement(IRStatement.Normal.Alloca(it))
            statement(IRStatement.Normal.Store(src = total, dest = it))
        }
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(cond)
        val index = next(IRType.I32).also { statement(IRStatement.Normal.Load(dest = it, src = loop)) }
        val location = next(IRType.Address(type)).also { statement(IRStatement.Normal.Index(it, parent, index)) }
        val condition = next(IRType.I1).also {
            statement(IRStatement.Normal.ICmp(it, IRCmpOp.SGE, index, IRType.I32 const 0))
        }
        statement(IRStatement.Terminate.Branch(cond = condition, then = body, els = end))
        enterNewBlock(body)
        val cur = next(type).also {
            statement(IRStatement.Normal.MallocArray(it, length[current]))
        }
        statement(IRStatement.Normal.Store(src = cur, dest = location))
        arraySugar(length, cur, current + 1)
        val next = next(IRType.I32).also {
            statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, index, IRType.I32 const 1))
        }
        statement(IRStatement.Normal.Store(src = next, dest = loop))
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(end)
    }

    private operator fun invoke(ast: ASTNode.Expression.NewArray): ExprResult {
        val type = TypeMap[ast.type] as? IRType.Array ?: error("unexpected non-array type")
        val length = ast.length.map { this(it).value }
        val ret = next(type).also {
            statement(IRStatement.Normal.MallocArray(it, length[0]))
        }
        arraySugar(length, ret, 1)
        return ret.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberAccess): ExprResult {
        val parent = this(ast.parent).value
        val variable = ast.reference
        val resultType = IRType.Address(TypeMap[variable.type])
        return next(resultType).also { statement(IRStatement.Normal.Member(it, parent, variable)) }.asAddress
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberFunction): ExprResult {
        val parent = this(ast.base).value
        return next(TypeMap[ast.type]).also {
            statement(
                IRStatement.Normal.Call(
                    result = it, function = FunctionMap[ast.reference],
                    args = listOf(parent) + FunctionMap[ast.reference].args.run { subList(1, size) }.zip(ast.parameters)
                        .map { (t, a) -> this(a).value.nullable(t) }
                ))
        }.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.Index): ExprResult {
        val parent = this(ast.parent).value
        val index = this(ast.child).value
        val llvmType = IRType.Address(TypeMap[ast.type])
        return next(llvmType).also { statement(IRStatement.Normal.Index(it, parent, index)) }.asAddress
    }

    private operator fun invoke(ast: ASTNode.Expression.Function): ExprResult {
        val args = if (ast.reference.base == null)
            FunctionMap[ast.reference].args.zip(ast.parameters).map { (t, a) -> this(a).value.nullable(t) }
        else
            listOf(thi ?: error("this unresolved unexpectedly")) +
                    FunctionMap[ast.reference].args.apply { subList(1, size) }.zip(ast.parameters)
                        .map { (t, a) -> this(a).value.nullable(t) }
        return next(TypeMap[ast.type]).also {
            statement(IRStatement.Normal.Call(it, FunctionMap[ast.reference], args))
        }.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.Suffix): ExprResult {
        val operand = this(ast.operand)
        val result = operand.value
        val after = next(IRType.I32).also {
            statement(
                IRStatement.Normal.ICalc(
                    it, when (ast.operator) {
                        MxSuffix.INC -> IRCalcOp.ADD
                        MxSuffix.DEC -> IRCalcOp.SUB
                    }, result, IRType.I32 const 1
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
            MxPrefix.INC, MxPrefix.DEC -> next(IRType.I32).also {
                statement(
                    IRStatement.Normal.ICalc(
                        it, when (ast.operator) {
                            MxPrefix.INC -> IRCalcOp.ADD
                            MxPrefix.DEC -> IRCalcOp.SUB
                            else -> error("")
                        }, rvalue, IRType.I32 const 1
                    )
                )
                statement(IRStatement.Normal.Store(src = it, dest = lvalue))
            }
            MxPrefix.L_NEG -> next(IRType.I1).also {
                statement(IRStatement.Normal.ICmp(it, IRCmpOp.EQ, rvalue, IRType.I32 const 0))
            }
            MxPrefix.INV -> next(IRType.I32).also {
                statement(IRStatement.Normal.ICalc(it, IRCalcOp.XOR, rvalue, IRType.I32 const -1))
            }
            MxPrefix.POS -> rvalue
            MxPrefix.NEG -> next(IRType.I32).also {
                statement(IRStatement.Normal.ICalc(it, IRCalcOp.SUB, IRType.I32 const 0, rvalue))
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
            val second = IRBlock(".short.$id.second")
            val result = IRBlock(".short.$id.end")
            statement(
                if (operation == Operation.BAnd) IRStatement.Terminate.Branch(lr, second, result)
                else IRStatement.Terminate.Branch(lr, result, second)
            )
            enterNewBlock(second)
            val rr = this(ast.rhs).value
            statement(IRStatement.Terminate.Jump(result))
            enterNewBlock(result)
            val ret = next(IRType.I1).also {
                statement(
                    IRStatement.Phi(
                        it,
                        listOf((IRType.I32 const if (operation == Operation.BAnd) 0 else 1) to current, rr to second)
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
                next(IRType.I32).also {
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
                next(IRType.I32).also {
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
                next(IRType.I1).also {
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
            Operation.BEqual, Operation.BNeq -> next(IRType.I1).also {
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
            Operation.SPlus -> next(IRType.String).also {
                statement(IRStatement.Normal.Call(it, IRFunction.External.StringConcatenate, listOf(lr, rr)))
            }
            Operation.SPlusI -> next(IRType.String).also {
                statement(IRStatement.Normal.Call(it, IRFunction.External.StringConcatenate, listOf(lr, rr)))
                statement(IRStatement.Normal.Store(src = it, dest = ll))
            }
            Operation.SLess, Operation.SLeq, Operation.SGreater, Operation.SGeq, Operation.SEqual, Operation.SNeq ->
                next(IRType.I1).also {
                    val raw = next(IRType.I1)
                    statement(
                        IRStatement.Normal.Call(
                            raw, when (operation) {
                                Operation.SLess -> IRFunction.External.StringLess
                                Operation.SLeq -> IRFunction.External.StringLeq
                                Operation.SGreater -> IRFunction.External.StringGreater
                                Operation.SGeq -> IRFunction.External.StringGeq
                                Operation.SEqual -> IRFunction.External.StringEqual
                                Operation.SNeq -> IRFunction.External.StringNeq
                                else -> error("")
                            }, listOf(lr, rr)
                        )
                    )
                    statement(IRStatement.Normal.ICmp(it, IRCmpOp.NE, raw, IRType.I32 const 0))
                }
            is Operation.PEqual -> next(IRType.I1).also {
                statement(
                    IRStatement.Normal.ICmp(
                        it, IRCmpOp.EQ, lr.nullable(TypeMap[operation.clazz]), rr.nullable(TypeMap[operation.clazz])
                    )
                )
            }
            is Operation.PNeq -> next(IRType.I1).also {
                statement(
                    IRStatement.Normal.ICmp(
                        it, IRCmpOp.NE, lr.nullable(TypeMap[operation.clazz]), rr.nullable(TypeMap[operation.clazz])
                    )
                )
            }
            is Operation.PAssign -> ll.also {
                statement(IRStatement.Normal.Store(src = rr.nullable(TypeMap[operation.clazz]), dest = ll))
            }
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
        return next(TypeMap[ast.type]).also {
            statement(IRStatement.Phi(it, listOf(thenValue to then, elsValue to els)))
        }.asValue
    }

    private operator fun invoke(ast: ASTNode.Expression.Identifier): ExprResult {
        val (type, variable) = ast.reference
        return when (type) {
            ReferenceType.Variable -> (local[variable] ?: GlobalMap[variable]).asAddress
            ReferenceType.Member -> {
                thi ?: error("unresolved identifier after semantic")
                val resultType = IRType.Address(TypeMap[variable.type])
                return next(resultType).also { statement(IRStatement.Normal.Member(it, thi!!, variable)) }.asAddress
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Expression.Constant.String): ExprResult {
        return LiteralMap[ast.value].asValue
    }
}