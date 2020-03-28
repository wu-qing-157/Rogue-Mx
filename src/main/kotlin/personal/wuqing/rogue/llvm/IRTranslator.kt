package personal.wuqing.rogue.llvm

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ast.ReferenceType
import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.grammar.MxVariable
import personal.wuqing.rogue.grammar.operator.MxPrefix
import personal.wuqing.rogue.grammar.operator.MxSuffix
import personal.wuqing.rogue.grammar.operator.Operation
import personal.wuqing.rogue.llvm.grammar.IRBlock
import personal.wuqing.rogue.llvm.grammar.IRCalcOp
import personal.wuqing.rogue.llvm.grammar.IRCmpOp
import personal.wuqing.rogue.llvm.grammar.IRFunction
import personal.wuqing.rogue.llvm.grammar.IRGlobal
import personal.wuqing.rogue.llvm.grammar.IRItem
import personal.wuqing.rogue.llvm.grammar.IRProgram
import personal.wuqing.rogue.llvm.grammar.IRStatement
import personal.wuqing.rogue.llvm.grammar.IRType
import personal.wuqing.rogue.llvm.map.FunctionMap
import personal.wuqing.rogue.llvm.map.GlobalMap
import personal.wuqing.rogue.llvm.map.LiteralMap
import personal.wuqing.rogue.llvm.map.TypeMap
import java.util.LinkedList

object IRTranslator {
    val toProcess = LinkedList<IRFunction.Declared>()

    operator fun invoke(ast: ASTNode.Program, main: MxFunction): IRProgram {
        ast.declarations.filterIsInstance<ASTNode.Declaration.Variable>().map { it.actual }.forEach {
            GlobalMap[it] = IRGlobal(
                IRItem.Global(IRType.Pointer(TypeMap[it.type]), it.name),
                when (it.type) {
                    MxType.Primitive.Int -> IRType.I32 const 0
                    MxType.Primitive.Bool -> IRType.I1 const 0
                    is MxType.Class, is MxType.Array, MxType.Primitive.String -> IRItem.Null(TypeMap[it.type])
                    MxType.Unknown, MxType.Void, MxType.Null -> error("unexpected global variable type")
                }
            )
        }
        FunctionMap[main]
        while (toProcess.isNotEmpty()) toProcess.poll().body
        return IRProgram(
            struct = TypeMap.all()
                .filterIsInstance<IRType.Pointer>()
                .map { it.base }
                .filterIsInstance<IRType.Class>(),
            global = GlobalMap.all() + LiteralMap.all(),
            function = FunctionMap.all().filterIsInstance<IRFunction.Declared>(),
            external = FunctionMap.all().filterIsInstance<IRFunction.External>()
        )
    }

    private var localCount = 0
    private fun next(type: IRType) = if (type is IRType.Void) IRItem.Void else IRItem.Local(type, ".${localCount++}")
    private val local = mutableMapOf<MxVariable, IRItem>()
    private val blocks = mutableListOf<IRBlock>()
    private val currentBlock get() = blocks.last()
    private val currentPhi get() = currentBlock.phi
    private val currentNormal get() = currentBlock.normal
    private val loopTarget = mutableMapOf<ASTNode.Statement.Loop, Pair<IRBlock, IRBlock>>()
    private var terminating = true
    private var returnType: IRType? = null
    private var thi: IRItem? = null

    private operator fun plusAssign(statement: IRStatement.Phi) {
        if (currentNormal.isNotEmpty()) error("non-phi statements before phi")
        if (!terminating) currentPhi += statement
    }

    private operator fun plusAssign(statement: IRStatement.Normal) {
        if (!terminating) currentNormal += statement
    }

    private operator fun plusAssign(statement: IRStatement.Terminate) {
        if (!terminating) currentBlock.terminate = statement
        terminating = true
    }

    private operator fun plusAssign(block: IRBlock) {
        if (!terminating) error("previous block not terminated")
        blocks += block
        terminating = false
    }

    private data class Value(val item: IRItem, val lvalue: Boolean) {
        fun rvalue() = if (lvalue) next((item.type as? IRType.Pointer ?: error("unexpected non-pointer")).base).also {
            IRTranslator += IRStatement.Normal.Load(dest = it, src = item)
        } else item
    }

    private infix fun IRItem.lvalue(lvalue: Boolean) = Value(this, lvalue)

    private fun IRItem.nullable(expect: IRType) = if (this is IRItem.Null) IRItem.Null(expect) else this

    private operator fun invoke(ast: ASTNode.Expression): Value = when (ast) {
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
        is ASTNode.Expression.This -> thi?.lvalue(false) ?: error("unresolved this after semantic")
        is ASTNode.Expression.Constant.Int -> IRType.I32 const ast.value lvalue false
        is ASTNode.Expression.Constant.String -> this(ast)
        is ASTNode.Expression.Constant.True -> IRType.I1 const 1 lvalue false
        is ASTNode.Expression.Constant.False -> IRType.I1 const 0 lvalue false
        is ASTNode.Expression.Constant.Null -> IRItem.Null(IRType.I8P) lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.NewObject): Value {
        val type = ast.baseType.type as? MxType.Class ?: error("new non-class type found after semantic")
        val llvmType = TypeMap[type] as? IRType.Pointer ?: error("invalid class type status")
        val classType = llvmType.base as? IRType.Class ?: error("unexpected non-class type")
        val size = classType.members.size
        val raw = next(IRType.I8P).also {
            this += IRStatement.Normal.Call(it, FunctionMap[MxFunction.Builtin.Malloc], listOf(IRType.I32 const size))
        }
        val cast = next(llvmType).also {
            this += IRStatement.Normal.Cast(from = raw, to = it)
        }
        for ((variable, index) in classType.members.delta) if (variable.declaration.init != null) {
            val value = this(variable.declaration.init).rvalue()
            val memberType = IRType.Pointer(TypeMap[variable.type])
            val member = next(memberType).also {
                this += IRStatement.Normal.Element(
                    result = it, src = cast, indices = listOf(IRType.I32 const 0, IRType.I32 const index)
                )
            }
            this += IRStatement.Normal.Store(src = value, dest = member)
        }
        val constructor =
            ast.baseType.type.functions["__constructor__"] ?: error("constructor not found after semantic")
        if (constructor !is MxFunction.Builtin.DefaultConstructor) {
            this += IRStatement.Normal.Call(
                result = IRItem.Void,
                function = FunctionMap[constructor],
                args = listOf(cast) + FunctionMap[constructor].args.run { subList(1, this.size) }.zip(ast.parameters)
                    .map { (t, a) -> this(a).rvalue().nullable(t) }
            )
        }
        return cast lvalue false
    }

    private fun arraySugar(length: List<IRItem>, parent: IRItem, current: Int) {
        if (current == length.size) return
        val type =
            (parent.type as? IRType.Pointer)?.base as? IRType.Pointer ?: error("unexpected non-array type")
        val childSize = type.base.size
        val id = localCount++
        val cond = IRBlock(".$id.array.cond")
        val body = IRBlock(".$id.array.body")
        val end = IRBlock(".$id.array.end")
        val total = next(IRType.I32).also {
            this += IRStatement.Normal.ICalc(it, IRCalcOp.SUB, length[current - 1], IRType.I32 const 0)
        }
        val loop = next(IRType.Pointer(IRType.I32)).also {
            this += IRStatement.Normal.Alloca(it)
            this += IRStatement.Normal.Store(src = total, dest = it)
        }
        this += IRStatement.Terminate.Jump(cond)
        this += cond
        val index = next(IRType.I32).also {
            this += IRStatement.Normal.Load(dest = it, src = loop)
        }
        val location = next(IRType.Pointer(type)).also {
            this += IRStatement.Normal.Element(it, parent, listOf(index))
        }
        val condition = next(IRType.I1).also {
            this += IRStatement.Normal.ICmp(it, IRCmpOp.SGE, index, IRType.I32 const 0)
        }
        this += IRStatement.Terminate.Branch(cond = condition, then = body, els = end)
        this += body
        val size = next(IRType.I32).also {
            this += IRStatement.Normal.ICalc(it, IRCalcOp.MUL, length[current], IRType.I32 const childSize)
        }
        val raw = next(IRType.I8P).also {
            this += IRStatement.Normal.Call(
                it, FunctionMap[MxFunction.Builtin.MallocArray], listOf(size, length[current])
            )
        }
        val cast = next(type).also {
            this += IRStatement.Normal.Cast(from = raw, to = it)
        }
        this += IRStatement.Normal.Store(src = cast, dest = location)
        arraySugar(length, cast, current + 1)
        val next = next(IRType.I32).also {
            this += IRStatement.Normal.ICalc(it, IRCalcOp.SUB, index, IRType.I32 const 1)
        }
        this += IRStatement.Normal.Store(src = next, dest = loop)
        this += IRStatement.Terminate.Jump(cond)
        this += end
    }

    private operator fun invoke(ast: ASTNode.Expression.NewArray): Value {
        val type = TypeMap[ast.type] as? IRType.Pointer ?: error("unexpected non-array type")
        val childSize = type.base.size
        val length = ast.length.map { this(it).rvalue() }
        val size = next(IRType.I32).also {
            this += IRStatement.Normal.ICalc(it, IRCalcOp.MUL, length[0], IRType.I32 const childSize)
        }
        val raw = next(IRType.I8P).also {
            this += IRStatement.Normal.Call(it, FunctionMap[MxFunction.Builtin.MallocArray], listOf(size, length[0]))
        }
        val cast = next(type).also {
            this += IRStatement.Normal.Cast(from = raw, to = it)
        }
        arraySugar(length, cast, 1)
        return cast lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberAccess): Value {
        val parent = this(ast.parent).rvalue()
        val parentType = TypeMap[ast.parent.type] as? IRType.Pointer ?: error("invalid class type status")
        val variable = ast.reference
        val resultType = IRType.Pointer(TypeMap[variable.type])
        val classType = parentType.base as? IRType.Class ?: error("unexpected non-class type")
        val index = classType.members.delta[variable] ?: error("member not arranged")
        return next(resultType).also {
            this += IRStatement.Normal.Element(it, parent, listOf(IRType.I32 const 0, IRType.I32 const index))
        } lvalue true
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberFunction): Value {
        val parent = this(ast.base).rvalue().let {
            if (ast.reference is MxFunction.Builtin.ArraySize) next(IRType.I8P).also { cast ->
                this += IRStatement.Normal.Cast(from = it, to = cast)
            } else it
        }
        return next(TypeMap[ast.type]).also {
            this += IRStatement.Normal.Call(
                result = it, function = FunctionMap[ast.reference],
                args = listOf(parent) + FunctionMap[ast.reference].args.run { subList(1, size) }.zip(ast.parameters)
                    .map { (t, a) -> this(a).rvalue().nullable(t) }
            )
        } lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.Index): Value {
        val parent = this(ast.parent).rvalue()
        val index = this(ast.child).rvalue()
        val llvmType = IRType.Pointer(TypeMap[ast.type])
        return next(llvmType).also {
            this += IRStatement.Normal.Element(it, parent, listOf(index))
        } lvalue true
    }

    private operator fun invoke(ast: ASTNode.Expression.Function): Value {
        val args = if (ast.reference.base == null)
            FunctionMap[ast.reference].args.zip(ast.parameters).map { (t, a) -> this(a).rvalue().nullable(t) }
        else
            listOf(thi ?: error("this unresolved unexpectedly")) +
                    FunctionMap[ast.reference].args.apply { subList(1, size) }.zip(ast.parameters)
                        .map { (t, a) -> this(a).rvalue().nullable(t) }
        return next(TypeMap[ast.type]).also {
            this += IRStatement.Normal.Call(it, FunctionMap[ast.reference], args)
        } lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.Suffix): Value {
        val operand = this(ast.operand)
        val result = operand.rvalue()
        val after = next(IRType.I32).also {
            this += IRStatement.Normal.ICalc(
                it, when (ast.operator) {
                    MxSuffix.INC -> IRCalcOp.ADD
                    MxSuffix.DEC -> IRCalcOp.SUB
                }, result, IRType.I32 const 1
            )
        }
        this += IRStatement.Normal.Store(src = after, dest = operand.item)
        return result lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.Prefix): Value {
        val operand = this(ast.operand)
        val lvalue = operand.item
        val rvalue = operand.rvalue()
        return when (ast.operator) {
            MxPrefix.INC, MxPrefix.DEC -> next(IRType.I32).also {
                this += IRStatement.Normal.ICalc(
                    it, when (ast.operator) {
                        MxPrefix.INC -> IRCalcOp.ADD
                        MxPrefix.DEC -> IRCalcOp.SUB
                        else -> error("")
                    }, rvalue, IRType.I32 const 1
                )
                this += IRStatement.Normal.Store(src = it, dest = lvalue)
            }
            MxPrefix.L_NEG -> next(IRType.I1).also {
                this += IRStatement.Normal.ICmp(it, IRCmpOp.EQ, rvalue, IRType.I32 const 0)
            }
            MxPrefix.INV -> next(IRType.I32).also {
                this += IRStatement.Normal.ICalc(it, IRCalcOp.XOR, rvalue, IRType.I32 const -1)
            }
            MxPrefix.POS -> rvalue
            MxPrefix.NEG -> next(IRType.I32).also {
                this += IRStatement.Normal.ICalc(it, IRCalcOp.SUB, IRType.I32 const 0, rvalue)
            }
        } lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.Binary): Value {
        val operation = ast.operator.operation(ast.lhs.type, ast.rhs.type)
            ?: error("unknown operation of binary operator after semantic")
        val lhs = this(ast.lhs)
        val ll = lhs.item
        val lr = lhs.rvalue()
        if (operation == Operation.BAnd || operation == Operation.BOr) {
            val current = currentBlock
            val id = localCount++
            val second = IRBlock(".$id.short.second")
            val result = IRBlock(".$id.short.end")
            this +=
                if (operation == Operation.BAnd) IRStatement.Terminate.Branch(lr, second, result)
                else IRStatement.Terminate.Branch(lr, result, second)
            this += second
            val rr = this(ast.rhs).rvalue()
            this += IRStatement.Terminate.Jump(result)
            this += result
            val ret = next(IRType.I1).also {
                this += IRStatement.Phi(
                    it, listOf((IRType.I32 const if (operation == Operation.BAnd) 0 else 1) to current, rr to second)
                )
            }
            return ret lvalue false
        }
        val rhs = this(ast.rhs)
        val rr = rhs.rvalue()
        return when (operation) {
            Operation.BAssign, Operation.IAssign, Operation.SAssign -> IRItem.Void.also {
                this += IRStatement.Normal.Store(rr, ll)
            }
            Operation.BAnd, Operation.BOr -> error("should already handled")
            Operation.Plus, Operation.Minus, Operation.Times, Operation.Div, Operation.Rem,
            Operation.IAnd, Operation.IOr, Operation.Xor, Operation.Shl, Operation.Shr, Operation.UShr ->
                next(IRType.I32).also {
                    this += IRStatement.Normal.ICalc(
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
                }
            Operation.PlusI, Operation.MinusI, Operation.TimesI, Operation.DivI, Operation.RemI,
            Operation.AndI, Operation.OrI, Operation.XorI, Operation.ShlI, Operation.ShrI, Operation.UShrI ->
                next(IRType.I32).let {
                    this += IRStatement.Normal.ICalc(
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
                    this += IRStatement.Normal.Store(src = it, dest = ll)
                    IRItem.Void
                }
            Operation.Less, Operation.Leq, Operation.Greater, Operation.Geq, Operation.IEqual, Operation.INeq ->
                next(IRType.I1).also {
                    this += IRStatement.Normal.ICmp(
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
                }
            Operation.BEqual, Operation.BNeq -> next(IRType.I1).also {
                this += IRStatement.Normal.ICmp(
                    it, when (operation) {
                        Operation.BEqual -> IRCmpOp.EQ
                        Operation.BNeq -> IRCmpOp.NE
                        else -> error("")
                    }, lr, rr
                )
            }
            Operation.SPlus -> next(IRType.string).also {
                this += IRStatement.Normal.Call(it, FunctionMap[MxFunction.Builtin.StringConcatenate], listOf(lr, rr))
            }
            Operation.SPlusI -> next(IRType.string).let {
                this += IRStatement.Normal.Call(it, FunctionMap[MxFunction.Builtin.StringConcatenate], listOf(lr, rr))
                this += IRStatement.Normal.Store(src = it, dest = ll)
                IRItem.Void
            }
            Operation.SLess, Operation.SLeq, Operation.SGreater, Operation.SGeq, Operation.SEqual, Operation.SNeq ->
                next(IRType.I1).also {
                    val raw = next(IRType.I8)
                    this += IRStatement.Normal.Call(
                        raw, FunctionMap[when (operation) {
                            Operation.SLess -> MxFunction.Builtin.StringLess
                            Operation.SLeq -> MxFunction.Builtin.StringLeq
                            Operation.SGreater -> MxFunction.Builtin.StringGreater
                            Operation.SGeq -> MxFunction.Builtin.StringGeq
                            Operation.SEqual -> MxFunction.Builtin.StringEqual
                            Operation.SNeq -> MxFunction.Builtin.StringNeq
                            else -> error("")
                        }], listOf(lr, rr)
                    )
                    this += IRStatement.Normal.ICmp(it, IRCmpOp.NE, raw, IRType.I32 const 0)
                }
            is Operation.PEqual -> next(IRType.I1).also {
                this += IRStatement.Normal.ICmp(
                    it, IRCmpOp.EQ, lr.nullable(TypeMap[operation.clazz]), rr.nullable(TypeMap[operation.clazz])
                )
            }
            is Operation.PNeq -> next(IRType.I1).also {
                this += IRStatement.Normal.ICmp(
                    it, IRCmpOp.NE, lr.nullable(TypeMap[operation.clazz]), rr.nullable(TypeMap[operation.clazz])
                )
            }
            is Operation.PAssign -> IRItem.Void.also {
                this += IRStatement.Normal.Store(src = rr.nullable(TypeMap[operation.clazz]), dest = ll)
            }
        } lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.Ternary): Value {
        val cond = this(ast.condition).rvalue()
        val id = localCount++
        val then = IRBlock(".$id.ternary.then")
        val els = IRBlock(".$id.ternary.else")
        val end = IRBlock(".$id.ternary.end")
        this += IRStatement.Terminate.Branch(cond, then, els)
        this += then
        val thenValue = this(ast.then).rvalue()
        this += IRStatement.Terminate.Jump(end)
        this += els
        val elsValue = this(ast.els).rvalue()
        this += IRStatement.Terminate.Jump(end)
        this += end
        return next(TypeMap[ast.type]).also {
            this += IRStatement.Phi(it, listOf(thenValue to then, elsValue to els))
        } lvalue false
    }

    private operator fun invoke(ast: ASTNode.Expression.Identifier): Value {
        val (type, variable) = ast.reference
        return when (type) {
            ReferenceType.Variable -> (local[variable] ?: GlobalMap[variable].name) lvalue true
            ReferenceType.Member -> {
                thi ?: error("unresolved identifier after semantic")
                val classType =
                    (thi!!.type as? IRType.Pointer)?.base as? IRType.Class ?: error("unexpected non-class type")
                val index = classType.members.delta[variable] ?: error("member not arranged")
                val resultType = IRType.Pointer(TypeMap[variable.type])
                return next(resultType).also {
                    this += IRStatement.Normal.Element(it, thi!!, listOf(IRType.I32 const 0, IRType.I32 const index))
                } lvalue true
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Expression.Constant.String): Value {
        val global = LiteralMap[ast.value]
        val cast = next(IRType.string).also {
            this += IRStatement.Normal.Cast(from = global.name, to = it)
        }
        return next(IRType.string).also {
            this += IRStatement.Normal.Call(
                it, FunctionMap[MxFunction.Builtin.StringLiteral],
                listOf(cast, IRType.I32 const ast.value.toByteArray().size)
            )
        } lvalue false
    }

    private operator fun invoke(ast: ASTNode.Statement) {
        if (terminating) return
        when (ast) {
            is ASTNode.Statement.Empty -> Unit
            is ASTNode.Statement.Block -> ast.statements.forEach { this(it) }
            is ASTNode.Statement.Expression -> this(ast.expression)
            is ASTNode.Statement.Variable -> this(ast)
            is ASTNode.Statement.If -> this(ast)
            is ASTNode.Statement.Loop.While -> this(ast)
            is ASTNode.Statement.Loop.For -> this(ast)
            is ASTNode.Statement.Continue -> this += IRStatement.Terminate.Jump(
                loopTarget[ast.loop]?.first ?: error("loop target is uninitialized unexpectedly")
            )
            is ASTNode.Statement.Break -> this += IRStatement.Terminate.Jump(
                loopTarget[ast.loop]?.second ?: error("loop target is uninitialized unexpectedly")
            )
            is ASTNode.Statement.Return -> this +=
                if (ast.expression == null) IRStatement.Terminate.Ret(IRItem.Void)
                else IRStatement.Terminate.Ret(
                    this(ast.expression).rvalue().nullable(returnType ?: error("unspecified return type"))
                )
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.Variable) {
        ast.variables.forEach { variable ->
            val type = TypeMap[variable.type.type]
            next(IRType.Pointer(type)).also {
                this += IRStatement.Normal.Alloca(it)
                local[variable.actual] = it
                variable.init?.let { init ->
                    this += IRStatement.Normal.Store(this(init).rvalue().nullable(TypeMap[variable.type.type]), it)
                } ?: run {
                    this += IRStatement.Normal.Store(
                        when (type) {
                            IRType.I32 -> IRType.I32 const 0
                            IRType.I8 -> IRType.I8 const 0
                            IRType.I1 -> IRType.I1 const 0
                            is IRType.Pointer -> IRItem.Null(type)
                            else -> error("variable pointing to illegal type")
                        }, it
                    )
                }
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.If) {
        val id = localCount++
        val then = IRBlock(".$id.if.then")
        val els = IRBlock(".$id.if.else")
        val end = IRBlock(".$id.if.end")
        val condition = this(ast.condition).rvalue()
        this += IRStatement.Terminate.Branch(condition, then, if (ast.els == null) end else els)
        this += then
        this(ast.then)
        this += IRStatement.Terminate.Jump(end)
        if (ast.els != null) {
            this += els
            this(ast.els)
            this += IRStatement.Terminate.Jump(end)
        }
        this += end
    }

    private operator fun invoke(ast: ASTNode.Statement.Loop.While) {
        val id = localCount++
        val cond = IRBlock(".$id.while.condition")
        val body = IRBlock(".$id.while.body")
        val end = IRBlock(".$id.while.end")
        loopTarget[ast] = cond to end
        this += IRStatement.Terminate.Jump(cond)
        this += cond
        val condition = this(ast.condition).rvalue()
        this += IRStatement.Terminate.Branch(condition, body, end)
        this += body
        this(ast.statement)
        this += IRStatement.Terminate.Jump(cond)
        this += end
    }

    private operator fun invoke(ast: ASTNode.Statement.Loop.For) {
        if (ast.init != null) this(ast.init)
        val id = localCount++
        val cond = IRBlock(".$id.for.cond")
        val body = IRBlock(".$id.for.body")
        val end = IRBlock(".$id.for.end")
        val step = IRBlock(".$id.for.step")
        loopTarget[ast] = step to end
        this += IRStatement.Terminate.Jump(cond)
        this += cond
        val condition = this(ast.condition).rvalue()
        this += IRStatement.Terminate.Branch(condition, body, end)
        this += body
        this(ast.statement)
        this += IRStatement.Terminate.Jump(step)
        this += step
        ast.step?.let { this(it) }
        this += IRStatement.Terminate.Jump(cond)
        this += end
    }

    fun processBody(function: IRFunction.Declared): MutableList<IRBlock> {
        localCount = 0
        if (function.member) thi = function.namedArgs.first()
        blocks.clear()
        returnType = function.ret
        val entry = IRBlock(".entry")
        if (function.name == "main") {
            this += IRBlock(".init_global")
            for ((variable, global) in GlobalMap.entries()) if (variable.declaration.init != null) {
                val value = this(variable.declaration.init).rvalue().nullable(global.name.type)
                this += IRStatement.Normal.Store(src = value, dest = global.name)
            }
            this += IRStatement.Terminate.Jump(entry)
        }
        this += entry
        function.ast.parameterList.zip(function.namedArgs.run { if (function.member) subList(1, size) else this })
            .forEach { (variable, arg) ->
                next(IRType.Pointer(arg.type)).also {
                    this += IRStatement.Normal.Alloca(it)
                    this += IRStatement.Normal.Store(src = arg, dest = it)
                    local[variable.actual] = it
                }
            }
        for (statement in function.ast.body.statements) this(statement)
        if (!terminating) this += when (function.ret) {
            IRType.I32 -> IRStatement.Terminate.Ret(IRType.I32 const 0)
            IRType.I8 -> IRStatement.Terminate.Ret(IRType.I8 const 0)
            IRType.I1 -> IRStatement.Terminate.Ret(IRType.I1 const 0)
            is IRType.Class -> error("returning class")
            is IRType.Pointer -> IRStatement.Terminate.Ret(IRItem.Null(function.ret))
            IRType.Void -> IRStatement.Terminate.Ret(IRItem.Void)
            is IRType.Vector -> error("returning vector")
            IRType.Null -> error("returning null")
        }
        return blocks.toMutableList()
    }
}
