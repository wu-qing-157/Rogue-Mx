package personal.wuqing.mxcompiler.llvm

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.ast.ReferenceType
import personal.wuqing.mxcompiler.grammar.MxFunction
import personal.wuqing.mxcompiler.grammar.MxType
import personal.wuqing.mxcompiler.grammar.MxVariable
import personal.wuqing.mxcompiler.grammar.operator.MxPrefix
import personal.wuqing.mxcompiler.grammar.operator.MxSuffix
import personal.wuqing.mxcompiler.grammar.operator.Operation
import personal.wuqing.mxcompiler.llvm.grammar.LLVMBlock
import personal.wuqing.mxcompiler.llvm.grammar.LLVMCalc
import personal.wuqing.mxcompiler.llvm.grammar.LLVMCmp
import personal.wuqing.mxcompiler.llvm.grammar.LLVMFunction
import personal.wuqing.mxcompiler.llvm.grammar.LLVMGlobal
import personal.wuqing.mxcompiler.llvm.grammar.LLVMName
import personal.wuqing.mxcompiler.llvm.grammar.LLVMProgram
import personal.wuqing.mxcompiler.llvm.grammar.LLVMStatement
import personal.wuqing.mxcompiler.llvm.grammar.LLVMType
import personal.wuqing.mxcompiler.llvm.map.FunctionMap
import personal.wuqing.mxcompiler.llvm.map.GlobalMap
import personal.wuqing.mxcompiler.llvm.map.LiteralMap
import personal.wuqing.mxcompiler.llvm.map.TypeMap
import java.util.LinkedList

object LLVMTranslator {
    val toProcess = LinkedList<LLVMFunction.Declared>()

    operator fun invoke(ast: ASTNode.Program, main: MxFunction): LLVMProgram {
        ast.declarations.filterIsInstance<ASTNode.Declaration.Variable>().map { it.actual }.forEach {
            GlobalMap[it] = LLVMGlobal(
                it.name, TypeMap[it.type], when (it.type) {
                    MxType.Primitive.Int, MxType.Primitive.Bool -> LLVMName.Const(0)
                    is MxType.Class, is MxType.Array, MxType.Primitive.String -> LLVMName.Null
                    MxType.Unknown, MxType.Void, MxType.Null -> throw Exception("unexpected global variable type")
                }
            )
        }
        FunctionMap[main]
        while (toProcess.isNotEmpty()) toProcess.poll().body
        return LLVMProgram(
            struct = TypeMap.all()
                .filterIsInstance<LLVMType.Pointer>()
                .map { it.type }
                .filterIsInstance<LLVMType.Class>(),
            global = GlobalMap.all() + LiteralMap.all(),
            function = FunctionMap.all().filterIsInstance<LLVMFunction.Declared>(),
            external = FunctionMap.all().filterIsInstance<LLVMFunction.External>()
        )
    }

    private var localCount = 0
    private fun nextName() = LLVMName.Local(".${localCount++}")
    private val local = mutableMapOf<MxVariable, Pair<LLVMType, LLVMName.Local>>()
    private val localBlocks = mutableListOf<LLVMBlock>()
    private val currentBlock get() = localBlocks.last()
    private val loopTarget = mutableMapOf<ASTNode.Statement.Loop, Pair<LLVMBlock, LLVMBlock>>()
    private val terminating
        get() = currentBlock.statements.lastOrNull()?.let { it is LLVMStatement.Terminating } == true
    private var thisReference: Pair<LLVMType.Pointer, LLVMName>? = null

    private operator fun plusAssign(statement: LLVMStatement) {
        currentBlock.statements += statement
    }

    private operator fun plusAssign(block: LLVMBlock) {
        localBlocks += block
    }

    private data class Value(val type: LLVMType, val lvalue: Boolean, val name: LLVMName) {
        fun rvalue() = if (lvalue) nextName().also {
            LLVMTranslator += LLVMStatement.Load(it, type, LLVMType.Pointer(type), name)
        } else name
    }

    private fun Value.processNull() = if (type == LLVMType.Null) LLVMName.Null else rvalue()

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
        is ASTNode.Expression.This -> thisReference?.let { Value(it.first, false, it.second) }
            ?: throw Exception("unresolved this after semantic")
        is ASTNode.Expression.Constant.Int -> Value(LLVMType.I32, false, LLVMName.Const(ast.value))
        is ASTNode.Expression.Constant.String -> this(ast)
        is ASTNode.Expression.Constant.True -> Value(LLVMType.I1, false, LLVMName.Const(1))
        is ASTNode.Expression.Constant.False -> Value(LLVMType.I1, false, LLVMName.Const(0))
        is ASTNode.Expression.Constant.Null -> Value(LLVMType.Null, false, LLVMName.Null)
    }

    private operator fun invoke(ast: ASTNode.Expression.NewObject): Value {
        val type = ast.baseType.type as? MxType.Class ?: throw Exception("new non-class type found after semantic")
        val llvmType = TypeMap[type] as? LLVMType.Pointer ?: throw Exception("invalid class type status")
        val classType = llvmType.type as? LLVMType.Class ?: throw Exception("unexpected non-class type")
        val size = classType.members.size
        val name = nextName()
        this += LLVMStatement.Call(
            name, LLVMType.string, FunctionMap[MxFunction.Builtin.Malloc].name, listOf(
                LLVMType.I32 to LLVMName.Const(size)
            )
        )
        val cast = nextName()
        this += LLVMStatement.Cast(cast, LLVMType.string, name, llvmType)
        for ((variable, index) in classType.members.delta) if (variable.declaration.init != null) {
            val value = this(variable.declaration.init).rvalue()
            val memberType = TypeMap[variable.type]
            val member = nextName()
            this += LLVMStatement.Element(
                member, classType, llvmType, cast, listOf(
                    LLVMType.I32 to LLVMName.Const(0), LLVMType.I32 to LLVMName.Const(index)
                )
            )
            this += LLVMStatement.Store(value, memberType, member, LLVMType.Pointer(memberType))
        }
        val constructor =
            ast.baseType.type.functions["__constructor__"] ?: throw Exception("constructor not found after semantic")
        if (constructor !is MxFunction.Builtin.DefaultConstructor) {
            val args = (FunctionMap[constructor].args zip ast.parameters).map { (t, p) ->
                t to this(p).processNull()
            } + (llvmType to cast)
            this += LLVMStatement.Call(null, LLVMType.Void, FunctionMap[constructor].name, args)
        }
        return Value(llvmType, false, cast)
    }

    private fun arraySugar(
        length: List<LLVMName>, parent: LLVMName, parentType: LLVMType.Pointer, current: Int
    ) {
        if (current == length.size) return
        val type = parentType.type as? LLVMType.Pointer ?: throw Exception("unexpected non-array type")
        val childSize = type.type.size
        val id = localCount++
        val cond = LLVMBlock("__condition__.$id")
        val body = LLVMBlock("__body__.$id")
        val end = LLVMBlock("__end__.$id")
        val total = nextName().also {
            this += LLVMStatement.ICalc(it, LLVMCalc.SUB, LLVMType.I32, length[current - 1], LLVMName.Const(1))
        }
        val loop = nextName().also {
            this += LLVMStatement.Alloca(it, LLVMType.I32)
            this += LLVMStatement.Store(total, LLVMType.I32, it, LLVMType.Pointer(LLVMType.I32))
        }
        this += LLVMStatement.Jump(cond.name)
        this += cond
        val index = nextName().also {
            this += LLVMStatement.Load(it, LLVMType.I32, LLVMType.Pointer(LLVMType.I32), loop)
        }
        // this += LLVMStatement.Call(null, LLVMType.Void, FunctionMap[MxFunction.Builtin.PrintlnInt].name, listOf(LLVMType.I32 to index))
        val location = nextName().also {
            this += LLVMStatement.Element(
                it, parentType.type, parentType, parent, listOf(LLVMType.I32 to index)
            )
        }
        val condition = nextName().also {
            this += LLVMStatement.ICmp(it, LLVMCmp.SGE, LLVMType.I32, index, LLVMName.Const(0))
        }
        this += LLVMStatement.Branch(LLVMType.I1, condition, body.name, end.name)
        this += body
        val size = nextName().also {
            this += LLVMStatement.ICalc(it, LLVMCalc.MUL, LLVMType.I32, length[current], LLVMName.Const(childSize))
        }
        val raw = nextName().also {
            this += LLVMStatement.Call(
                it, LLVMType.string, FunctionMap[MxFunction.Builtin.MallocArray].name, listOf(
                    LLVMType.I32 to size, LLVMType.I32 to length[current]
                )
            )
        }
        val cast = nextName().also {
            this += LLVMStatement.Cast(it, LLVMType.string, raw, type)
        }
        this += LLVMStatement.Store(cast, type, location, parentType)
        arraySugar(length, cast, type, current + 1)
        val next = nextName().also {
            this += LLVMStatement.ICalc(it, LLVMCalc.SUB, LLVMType.I32, index, LLVMName.Const(1))
        }
        this += LLVMStatement.Store(next, LLVMType.I32, loop, LLVMType.Pointer(LLVMType.I32))
        this += LLVMStatement.Jump(cond.name)
        this += end
    }

    private operator fun invoke(ast: ASTNode.Expression.NewArray): Value {
        // this += LLVMStatement.Call(null, LLVMType.Void, FunctionMap[MxFunction.Builtin.PrintlnInt].name, listOf(LLVMType.I32 to LLVMName.Const(12300)))
        val type = TypeMap[ast.type] as? LLVMType.Pointer ?: throw Exception("unexpected non-array type")
        val childSize = type.type.size
        val length = ast.length.map { this(it).rvalue() }
        val size = nextName().also {
            this += LLVMStatement.ICalc(it, LLVMCalc.MUL, LLVMType.I32, length[0], LLVMName.Const(childSize))
        }
        val raw = nextName().also {
            this += LLVMStatement.Call(
                it, LLVMType.string, FunctionMap[MxFunction.Builtin.MallocArray].name, listOf(
                    LLVMType.I32 to size, LLVMType.I32 to length[0]
                )
            )
        }
        val cast = nextName().also {
            this += LLVMStatement.Cast(it, LLVMType.string, raw, type)
        }
        arraySugar(length, cast, type, 1)
        return Value(type, false, cast)
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberAccess): Value {
        val parent = this(ast.parent).rvalue()
        val parentType = TypeMap[ast.parent.type] as? LLVMType.Pointer
            ?: throw Exception("invalid class type status")
        val variable = ast.reference
        val resultType = TypeMap[variable.type]
        val classType = parentType.type as? LLVMType.Class ?: throw Exception("unexpected non-class type")
        val index = classType.members.delta[variable] ?: throw Exception("member not arranged")
        val name = nextName()
        this += LLVMStatement.Element(
            name, parentType.type, parentType, parent, listOf(
                LLVMType.I32 to LLVMName.Const(0),
                LLVMType.I32 to LLVMName.Const(index)
            )
        )
        return Value(resultType, true, name)
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberFunction): Value {
        val baseType = TypeMap[ast.base.type]
            .takeIf { ast.reference !is MxFunction.Builtin.ArraySize } ?: LLVMType.string
        val parent = this(ast.base).rvalue().let {
            if (ast.reference is MxFunction.Builtin.ArraySize) nextName().also { cast ->
                this += LLVMStatement.Cast(cast, TypeMap[ast.base.type], it, LLVMType.string)
            } else it
        }
        val args = (FunctionMap[ast.reference].args zip ast.parameters).map { (t, p) ->
            t to this(p).processNull()
        } + (baseType to parent)
        val name = nextName()
        val llvmType = TypeMap[ast.type]
        this += LLVMStatement.Call(
            name.takeIf { llvmType != LLVMType.Void }, llvmType, FunctionMap[ast.reference].name, args
        )
        return Value(llvmType, false, name)
    }

    private operator fun invoke(ast: ASTNode.Expression.Index): Value {
        val parent = this(ast.parent).rvalue()
        val index = this(ast.child).rvalue()
        val llvmType = TypeMap[ast.type]
        val name = nextName()
        this += LLVMStatement.Element(
            name, llvmType, LLVMType.Pointer(llvmType), parent, listOf(
                LLVMType.I32 to index
            )
        )
        return Value(llvmType, true, name)
    }

    private operator fun invoke(ast: ASTNode.Expression.Function): Value {
        val args = (FunctionMap[ast.reference].args zip ast.parameters).map { (t, p) ->
            t to this(p).processNull()
        }.let { if (ast.reference.base != null) it + thisReference!! else it }
        val name = nextName()
        val llvmType = TypeMap[ast.type]
        this += LLVMStatement.Call(
            name.takeIf { llvmType != LLVMType.Void },
            llvmType,
            FunctionMap[ast.reference].name,
            args
        )
        return Value(llvmType, false, name)
    }

    private operator fun invoke(ast: ASTNode.Expression.Suffix): Value {
        val operand = this(ast.operand)
        val name = operand.rvalue()
        val after = nextName()
        this += LLVMStatement.ICalc(
            after, when (ast.operator) {
                MxSuffix.INC -> LLVMCalc.ADD
                MxSuffix.DEC -> LLVMCalc.SUB
            }, LLVMType.I32, name, LLVMName.Const(1)
        )
        this += LLVMStatement.Store(
            after, LLVMType.I32, operand.name, LLVMType.Pointer(
                LLVMType.I32
            )
        )
        return Value(LLVMType.I32, false, name)
    }

    private operator fun invoke(ast: ASTNode.Expression.Prefix): Value {
        val operand = this(ast.operand)
        val lvalue = operand.name
        val rvalue = operand.rvalue()
        val name = nextName()
        return when (ast.operator) {
            MxPrefix.INC -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.ADD, LLVMType.I32, rvalue, LLVMName.Const(1))
                this += LLVMStatement.Store(
                    name, LLVMType.I32, lvalue, LLVMType.Pointer(
                        LLVMType.I32
                    )
                )
                Value(LLVMType.I32, true, lvalue)
            }
            MxPrefix.DEC -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SUB, LLVMType.I32, rvalue, LLVMName.Const(1))
                this += LLVMStatement.Store(
                    name, LLVMType.I32, lvalue, LLVMType.Pointer(
                        LLVMType.I32
                    )
                )
                Value(LLVMType.I32, true, lvalue)
            }
            MxPrefix.L_NEG -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.EQ, LLVMType.I1, rvalue, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            MxPrefix.INV -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.XOR, LLVMType.I32, rvalue, LLVMName.Const(-1))
                Value(LLVMType.I32, false, name)
            }
            MxPrefix.POS -> {
                this += LLVMStatement.Store(
                    name, LLVMType.I32, rvalue, LLVMType.Pointer(
                        LLVMType.I32
                    )
                )
                Value(LLVMType.I32, false, name)
            }
            MxPrefix.NEG -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SUB, LLVMType.I32, LLVMName.Const(0), rvalue)
                Value(LLVMType.I32, false, name)
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Expression.Binary): Value {
        val operation = ast.operator.operation(ast.lhs.type, ast.rhs.type)
            ?: throw Exception("unknown operation of binary operator after semantic")
        val lhs = this(ast.lhs)
        val ll = lhs.name
        val lr = lhs.rvalue()
        val name = nextName()
        if (operation == Operation.BAnd || operation == Operation.BOr) {
            val current = currentBlock.name
            val id = localCount++
            val second = LLVMBlock("__second__.$id")
            val result = LLVMBlock("__result__.$id")
            this += if (operation == Operation.BAnd) LLVMStatement.Branch(LLVMType.I1, lr, second.name, result.name)
            else LLVMStatement.Branch(LLVMType.I1, lr, result.name, second.name)
            this += second
            val rr = this(ast.rhs).rvalue()
            this += LLVMStatement.Jump(result.name)
            this += result
            this += LLVMStatement.Phi(
                name, LLVMType.I1, listOf(
                    LLVMName.Const(if (operation == Operation.BAnd) 0 else 1) to current, rr to second.name
                )
            )
            return Value(LLVMType.I1, false, name)
        }
        val rhs = this(ast.rhs)
        val rr = rhs.rvalue()
        return when (operation) {
            Operation.Plus -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.ADD, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.SPlus -> {
                this += LLVMStatement.Call(
                    name, LLVMType.string, FunctionMap[MxFunction.Builtin.StringConcatenate].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                Value(LLVMType.string, false, name)
            }
            Operation.Minus -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SUB, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Times -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.MUL, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Div -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SDIV, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Rem -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SREM, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.BAnd, Operation.BOr -> throw Exception("should already handled")
            Operation.IAnd -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.AND, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.IOr -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.OR, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Xor -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.XOR, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Shl -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SHL, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.UShr -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.LSHR, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Shr -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.ASHR, LLVMType.I32, lr, rr)
                Value(LLVMType.I32, false, name)
            }
            Operation.Less -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.SLT, LLVMType.I32, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.SLess -> {
                val result = nextName()
                this += LLVMStatement.Call(
                    result, LLVMType.I8, FunctionMap[MxFunction.Builtin.StringLess].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I8, result, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            Operation.Leq -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.SLE, LLVMType.I32, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.SLeq -> {
                val result = nextName()
                this += LLVMStatement.Call(
                    result, LLVMType.I8, FunctionMap[MxFunction.Builtin.StringLeq].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I8, result, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            Operation.Greater -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.SGT, LLVMType.I32, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.SGreater -> {
                val result = nextName()
                this += LLVMStatement.Call(
                    result, LLVMType.I8, FunctionMap[MxFunction.Builtin.StringGreater].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I8, result, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            Operation.Geq -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.SGE, LLVMType.I32, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.SGeq -> {
                val result = nextName()
                this += LLVMStatement.Call(
                    result, LLVMType.I8, FunctionMap[MxFunction.Builtin.StringGeq].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I8, result, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            Operation.BEqual -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.EQ, LLVMType.I1, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.IEqual -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.EQ, LLVMType.I32, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.SEqual -> {
                val result = nextName()
                this += LLVMStatement.Call(
                    result, LLVMType.I8, FunctionMap[MxFunction.Builtin.StringEqual].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I8, result, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            Operation.BNeq -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I1, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.INeq -> {
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I32, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            Operation.SNeq -> {
                val result = nextName()
                this += LLVMStatement.Call(
                    result, LLVMType.I8, FunctionMap[MxFunction.Builtin.StringNeq].name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, LLVMType.I8, result, LLVMName.Const(0))
                Value(LLVMType.I1, false, name)
            }
            Operation.BAssign -> {
                this += LLVMStatement.Store(rr, LLVMType.I1, ll, LLVMType.Pointer(LLVMType.I1))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.IAssign -> {
                this += LLVMStatement.Store(rr, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.SAssign -> {
                this += LLVMStatement.Store(rr, LLVMType.string, ll, LLVMType.Pointer(LLVMType.string))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.PlusI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.ADD, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.SPlusI -> {
                this += LLVMStatement.Call(
                    name, LLVMType.string, LLVMFunction.External.StringConcatenate.name, listOf(
                        LLVMType.string to lr, LLVMType.string to rr
                    )
                )
                this += LLVMStatement.Store(name, LLVMType.string, ll, LLVMType.Pointer(LLVMType.string))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.MinusI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SUB, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.TimesI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.MUL, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.DivI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SDIV, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.RemI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SREM, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.AndI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.AND, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.OrI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.OR, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.XorI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.XOR, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.ShlI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.SHL, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.UShrI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.LSHR, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            Operation.ShrI -> {
                this += LLVMStatement.ICalc(name, LLVMCalc.ASHR, LLVMType.I32, lr, rr)
                this += LLVMStatement.Store(name, LLVMType.I32, ll, LLVMType.Pointer(LLVMType.I32))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
            is Operation.PEqual -> {
                val llvmType = TypeMap[ast.lhs.type]
                this += LLVMStatement.ICmp(name, LLVMCmp.EQ, llvmType, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            is Operation.PNeq -> {
                val llvmType = TypeMap[ast.lhs.type]
                this += LLVMStatement.ICmp(name, LLVMCmp.NE, llvmType, lr, rr)
                Value(LLVMType.I1, false, name)
            }
            is Operation.PAssign -> {
                val llvmType = TypeMap[ast.lhs.type]
                this += LLVMStatement.Store(rhs.processNull(), llvmType, ll, LLVMType.Pointer(llvmType))
                Value(LLVMType.Void, false, LLVMName.Void)
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Expression.Ternary): Value {
        val cond = this(ast.condition).rvalue()
        val id = localCount++
        val then = LLVMBlock("__then__.$id")
        val els = LLVMBlock("__else__.$id")
        val end = LLVMBlock("__end__.$id")
        this += LLVMStatement.Branch(LLVMType.I1, cond, then.name, els.name)
        this += then
        val thenValue = this(ast.then).rvalue()
        this += LLVMStatement.Jump(end.name)
        this += els
        val elsValue = this(ast.els).rvalue()
        this += LLVMStatement.Jump(end.name)
        this += end
        val name = nextName()
        this += LLVMStatement.Phi(name, TypeMap[ast.type], listOf(thenValue to then.name, elsValue to els.name))
        return Value(TypeMap[ast.type], false, name)
    }

    private operator fun invoke(ast: ASTNode.Expression.Identifier): Value {
        val (type, variable) = ast.reference
        return when (type) {
            ReferenceType.Variable ->
                local[variable]?.let {
                    Value(it.first, true, it.second)
                } ?: GlobalMap[variable].let {
                    Value(it.type, true, it.name)
                }
            ReferenceType.Member -> {
                val (thisType, thisName) = thisReference ?: throw Exception("unresolved identifier after semantic")
                val classType = thisType.type as? LLVMType.Class ?: throw Exception("unexpected non-class type")
                val index = classType.members.delta[variable] ?: throw Exception("member not arranged")
                val resultType = TypeMap[variable.type]
                val name = nextName()
                this += LLVMStatement.Element(
                    name, thisType.type, thisType, thisName, listOf(
                        LLVMType.I32 to LLVMName.Const(0),
                        LLVMType.I32 to LLVMName.Const(index)
                    )
                )
                Value(resultType, true, name)
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Expression.Constant.String): Value {
        val global = LiteralMap[ast.value]
        val cast = nextName().also {
            this += LLVMStatement.Cast(it, LLVMType.Pointer(global.type), global.name, LLVMType.string)
        }
        val name = nextName().also {
            this += LLVMStatement.Call(it, LLVMType.string, FunctionMap[MxFunction.Builtin.StringLiteral].name, listOf(
                LLVMType.string to cast, LLVMType.I32 to LLVMName.Const(ast.value.toByteArray().size)
            ))
        }
        return Value(LLVMType.string, false, name)
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
            is ASTNode.Statement.Continue -> this += LLVMStatement.Jump(
                loopTarget[ast.loop]?.first?.name ?: throw Exception("loop target is uninitialized unexpectedly")
            )
            is ASTNode.Statement.Break -> this += LLVMStatement.Jump(
                loopTarget[ast.loop]?.second?.name ?: throw Exception("loop target is uninitialized unexpectedly")
            )
            is ASTNode.Statement.Return -> this +=
                if (ast.expression == null) LLVMStatement.Ret(LLVMType.Void, null)
                else LLVMStatement.Ret(TypeMap[ast.expression.type], this(ast.expression).rvalue())
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.Variable) {
        ast.variables.forEach { variable ->
            val name = LLVMName.Local(variable.name)
            val type = TypeMap[variable.type.type]
            local[variable.actual] = type to name
            this += LLVMStatement.Alloca(name, type)
            if (variable.init != null) {
                val initName = this(variable.init).processNull()
                this += LLVMStatement.Store(initName, type, name, LLVMType.Pointer(type))
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.If) {
        val id = localCount++
        val then = LLVMBlock("__then__.$id")
        val els = LLVMBlock("__else__.$id")
        val end = LLVMBlock("__end__.$id")
        val condition = this(ast.condition).rvalue()
        this += LLVMStatement.Branch(LLVMType.I1, condition, then.name, if (ast.els == null) end.name else els.name)
        this += then
        this(ast.then)
        this += LLVMStatement.Jump(end.name)
        if (ast.els != null) {
            this += els
            this(ast.els)
            this += LLVMStatement.Jump(end.name)
        }
        this += end
    }

    private operator fun invoke(ast: ASTNode.Statement.Loop.While) {
        val id = localCount++
        val cond = LLVMBlock("__condition__.$id")
        val body = LLVMBlock("__body__.$id")
        val end = LLVMBlock("__end__.$id")
        loopTarget[ast] = cond to end
        this += LLVMStatement.Jump(cond.name)
        this += cond
        val condition = this(ast.condition).rvalue()
        this += LLVMStatement.Branch(LLVMType.I1, condition, body.name, end.name)
        this += body
        this(ast.statement)
        this += LLVMStatement.Jump(cond.name)
        this += end
    }

    private operator fun invoke(ast: ASTNode.Statement.Loop.For) {
        if (ast.init != null) this(ast.init)
        val id = localCount++
        val cond = LLVMBlock("__condition__.$id")
        val body = LLVMBlock("__body__.$id")
        val end = LLVMBlock("__end__.$id")
        val step = LLVMBlock("__step__.$id")
        loopTarget[ast] = step to end
        this += LLVMStatement.Jump(cond.name)
        this += cond
        val condition = this(ast.condition).rvalue()
        this += LLVMStatement.Branch(LLVMType.I1, condition, body.name, end.name)
        this += body
        this(ast.statement)
        this += LLVMStatement.Jump(step.name)
        this += step
        ast.step?.let { this(it) }
        this += LLVMStatement.Jump(cond.name)
        this += end
    }

    fun processBody(function: LLVMFunction.Declared): List<LLVMBlock> {
        localCount = 0
        if (function.member) thisReference = (function.args.last() as? LLVMType.Pointer
            ?: throw Exception("unexpected non-class type")) to function.argName.last()
        localBlocks.clear()
        val entry = LLVMBlock("__entry__")
        if (function.name == LLVMName.Global("main")) {
            this += LLVMBlock("__init__global__")
            for ((variable, global) in GlobalMap.entries()) if (variable.declaration.init != null) {
                val value = this(variable.declaration.init).rvalue()
                val type = global.type
                this += LLVMStatement.Store(value, type, global.name, LLVMType.Pointer(global.type))
            }
            this += LLVMStatement.Jump(entry.name)
        }
        this += entry
        function.ast.parameterList.withIndex().forEach { (i, variable) ->
            val t = function.args[i]
            val n = function.argName[i]
            val name = nextName()
            this += LLVMStatement.Alloca(name, t)
            this += LLVMStatement.Store(n, t, name, LLVMType.Pointer(t))
            local[variable.actual] = t to name
        }
        for (statement in function.ast.body.statements) this(statement)
        this += when (function.ret) {
            LLVMType.I32 -> LLVMStatement.Ret(LLVMType.I32, LLVMName.Const(0))
            LLVMType.I8 -> LLVMStatement.Ret(LLVMType.I8, LLVMName.Const(0))
            LLVMType.I1 -> LLVMStatement.Ret(LLVMType.I1, LLVMName.Const(0))
            is LLVMType.Class -> throw Exception("returning class")
            is LLVMType.Pointer -> LLVMStatement.Ret(function.ret, LLVMName.Null)
            LLVMType.Void -> LLVMStatement.Ret(LLVMType.Void, null)
            is LLVMType.Vector -> throw Exception("returning vector")
            LLVMType.Null -> throw Exception("returning null")
        }
        return localBlocks.toList().apply {
            forEach {
                if (it.statements.last() !is LLVMStatement.Terminating)
                    throw Exception("unterminated block found unexpectedly")
            }
        }
    }
}
