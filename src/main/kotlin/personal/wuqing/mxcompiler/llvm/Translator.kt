package personal.wuqing.mxcompiler.llvm

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.grammar.Function
import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.grammar.Variable

object Translator {
    private val type = mutableMapOf<Type, LLVMType>()
    private val function = mutableMapOf<Function, LLVMFunction>()
    private val global = mutableMapOf<Variable, LLVMGlobal>()

    operator fun get(t: Type): LLVMType = type[t] ?: when (t) {
        Type.Primitive.Int -> LLVMType.I(32)
        Type.Primitive.Bool -> LLVMType.I(8)
        Type.Primitive.String -> LLVMType.String
        Type.Null -> LLVMType.I(64)
        Type.Void -> LLVMType.Void
        Type.Unknown -> throw Exception("type <unknown> found after semantic")
        is Type.Class -> LLVMType.Class(t.name)
        is Type.Array -> LLVMType.Pointer(this[t.base])
    }.also { type[t] = it }.also {
        if (t is Type.Class) (it as LLVMType.Class).init(MemberArrangement(t))
    }

    private fun Function.llvmName() = when {
        base != null -> "$base.$name"
        name == "main" -> "main"
        else -> "__toplevel__.$name"
    }

    private operator fun get(f: Function) = function[f] ?: if (f is Function.Builtin)
        when (f) {
            Function.Builtin.Print -> LLVMFunction.External.Print
            Function.Builtin.Println -> LLVMFunction.External.Println
            Function.Builtin.PrintInt -> LLVMFunction.External.PrintInt
            Function.Builtin.PrintlnInt -> LLVMFunction.External.PrintlnInt
            Function.Builtin.GetString -> LLVMFunction.External.GetString
            Function.Builtin.GetInt -> LLVMFunction.External.GetInt
            Function.Builtin.ToString -> LLVMFunction.External.ToString
            Function.Builtin.StringLength -> LLVMFunction.External.StringLength
            Function.Builtin.StringParseInt -> LLVMFunction.External.StringParseInt
            is Function.Builtin.ArraySize -> LLVMFunction.External.ArraySize
            is Function.Builtin.DefaultConstructor -> LLVMFunction.External.Empty
            Function.Builtin.StringOrd -> LLVMFunction.External.StringOrd
            Function.Builtin.StringSubstring -> LLVMFunction.External.StringSubstring
        }
    else
        when (f) {
            is Function.Top -> LLVMFunction.Declared(
                this[f.result], f.llvmName(), f.parameters.map { this[it] }, f.def
            )
            is Function.Member -> LLVMFunction.Declared(
                this[f.result], f.llvmName(),
                (f.parameters + f.base).map { this[it] }, f.def
            )
            is Function.Builtin -> throw Exception("declared function resolved as builtin")
        }

    operator fun get(g: Variable) = global[g] ?: LLVMGlobal(g.name, this[g.type], LLVMName.Const(0))

    operator fun invoke(ast: ASTNode.Program): LLVMProgram {
        val classes = ast.declarations.filterIsInstance<ASTNode.Declaration.Class>().map {
            this[it.actual] as? LLVMType.Class ?: throw Exception("unexpected non-class type")
        }
        val globals = ast.declarations.filterIsInstance<ASTNode.Declaration.Variable>().map { this[it.actual] }
        val functions = ast.declarations.filterIsInstance<ASTNode.Declaration.Function>().map {
            this[it.actual] as? LLVMFunction.Declared ?: throw Exception("unexpected external function")
        }
        functions.forEach { it.body }
        return LLVMProgram(classes, globals, functions)
    }

    private var localCount = 0
    private val local = mutableMapOf<Variable, LLVMName.Local>()
    private val localBlocks = mutableListOf<LLVMBlock>()
    private val blockName get() = currentBlock.name
    private var currentBlock = LLVMBlock("this.name.should.not.appear.in.llvm.result")
        set(value) {
            field = value
            localBlocks += currentBlock
        }
    private val loopTarget = mutableMapOf<ASTNode.Statement.Loop, Pair<LLVMName.Local, LLVMName.Local>>()
    private var thisReference: Pair<LLVMType, LLVMName>? = null
    private fun newBlock() {
        currentBlock = LLVMBlock("${localCount++}")
    }

    private operator fun plusAssign(statement: LLVMStatement) {
        if (currentBlock.statements.lastOrNull()?.let { it is LLVMStatement.Terminating } != true)
            currentBlock.statements += statement
    }

    private operator fun invoke(ast: ASTNode.Expression): LLVMName = when (ast) {
        is ASTNode.Expression.NewObject -> this(ast)
        is ASTNode.Expression.NewArray -> this(ast)
        is ASTNode.Expression.MemberAccess -> this(ast)
        is ASTNode.Expression.MemberFunction -> this(ast)
        is ASTNode.Expression.Function -> this(ast)
        is ASTNode.Expression.Index -> TODO()
        is ASTNode.Expression.Suffix -> TODO()
        is ASTNode.Expression.Prefix -> TODO()
        is ASTNode.Expression.Binary -> TODO()
        is ASTNode.Expression.Ternary -> TODO()
        is ASTNode.Expression.Identifier -> TODO()
        is ASTNode.Expression.This -> thisReference?.second ?: throw Exception("unresolved this after semantic")
        is ASTNode.Expression.Constant.Int -> LLVMName.Const(ast.value)
        is ASTNode.Expression.Constant.String -> TODO()
        is ASTNode.Expression.Constant.True -> LLVMName.Const(1)
        is ASTNode.Expression.Constant.False -> LLVMName.Const(0)
        is ASTNode.Expression.Constant.Null -> LLVMName.Const(0)
    }

    private operator fun invoke(ast: ASTNode.Expression.NewObject): LLVMName {
        val type = ast.baseType.type as? Type.Class ?: throw Exception("new non-class type found after semantic")
        val llvmType = this[type] as? LLVMType.Class ?: throw Exception("class type mapped to non-class LLVM type")
        val size = llvmType.members.size
        val name = LLVMName.Local("${localCount++}")
        this += LLVMStatement.Call(
            result = name,
            type = LLVMType.Pointer(LLVMType.I(8)),
            name = LLVMFunction.External.Malloc.name,
            args = listOf(LLVMType.I(64) to LLVMName.Const(size))
        )
        val constructor =
            ast.baseType.type.functions["__constructor__"] ?: throw Exception("constructor not found after semantic")
        if (constructor !is Function.Builtin.DefaultConstructor) {
            this += LLVMStatement.Call(
                result = null,
                type = LLVMType.Void,
                name = this[constructor].name,
                args = listOf(LLVMType.Pointer(llvmType) to name)
            )
        }
        return name
    }

    private operator fun invoke(ast: ASTNode.Expression.NewArray): LLVMName = TODO("$ast")

    private operator fun invoke(ast: ASTNode.Expression.MemberAccess): LLVMName {
        val parent = this(ast.parent)
        val llvmType = this[ast.parent.type] as? LLVMType.Class
            ?: throw Exception("access member of non-class type found after semantic")
        val variable = ast.reference
        val index = llvmType.members.delta[variable] ?: throw Exception("member not arranged")
        val name = LLVMName.Local("${localCount++}")
        this += LLVMStatement.Element(
            name, this[variable.type], llvmType, parent, listOf(
                LLVMType.I(32) to LLVMName.Const(0),
                LLVMType.I(32) to LLVMName.Const(index)
            )
        )
        return name
    }

    private operator fun invoke(ast: ASTNode.Expression.MemberFunction): LLVMName {
        val parent = this(ast.base)
        val args = ast.parameters.map { this[it.type] to this(it) }
        val name = LLVMName.Local("${localCount++}")
        val baseType = ast.base.type
        this += LLVMStatement.Call(
            name, this[ast.type], LLVMName.Global("$baseType.${ast.name}"),
            args + (this[baseType] to parent)
        )
        return name
    }

    private operator fun invoke(ast: ASTNode.Expression.Function): LLVMName {
        val args = ast.parameters.map { this[it.type] to this(it) }.let {
            if (ast.reference.base != null)
                it + (thisReference ?: throw Exception("unresolved this after semantic"))
            else
                it
        }
        val name = LLVMName.Local("${localCount++}")
        this += LLVMStatement.Call(name, this[ast.type], LLVMName.Global(ast.reference.llvmName()), args)
        return name
    }

    private operator fun invoke(ast: ASTNode.Statement) {
        when (ast) {
            is ASTNode.Statement.Empty -> Unit
            is ASTNode.Statement.Block -> ast.statements.forEach { this(it) }
            is ASTNode.Statement.Expression -> this(ast.expression)
            is ASTNode.Statement.Variable -> this(ast)
            is ASTNode.Statement.If -> this(ast)
            is ASTNode.Statement.Loop.While -> this(ast)
            is ASTNode.Statement.Loop.For -> this(ast)
            is ASTNode.Statement.Continue -> this += LLVMStatement.Jump(
                loopTarget[ast.loop]?.first ?: throw Exception("loop target is uninitialized unexpectedly")
            )
            is ASTNode.Statement.Break -> this += LLVMStatement.Jump(
                loopTarget[ast.loop]?.second ?: throw Exception("loop target is uninitialized unexpectedly")
            )
            is ASTNode.Statement.Return -> this += if (ast.expression == null)
                LLVMStatement.Ret(LLVMType.Void, null)
            else
                LLVMStatement.Ret(this[ast.expression.type], this(ast.expression))
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.Variable) {
        ast.variables.forEach {
            val name = LLVMName.Local(it.name)
            val type = this[it.type.type]
            local[it.actual] = name
            this += LLVMStatement.Alloca(name, type)
            if (it.init != null) {
                val initName = this(it.init)
                this += LLVMStatement.Store(initName, LLVMType.Pointer(type), name, type)
            }
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.If) {
        val condition = this(ast.condition)
        val block = currentBlock
        newBlock()
        val thenStartName = blockName
        this(ast.then)
        val thenEndBlock = currentBlock
        newBlock()
        if (ast.else_ == null) {
            thenEndBlock.statements += LLVMStatement.Jump(blockName)
            block.statements +=
                LLVMStatement.Branch(this[Type.Primitive.Bool], condition, blockName, thenStartName)
        } else {
            val elseStartName = blockName
            this(ast.else_)
            val elseEndBlock = currentBlock
            newBlock()
            thenEndBlock.statements += LLVMStatement.Jump(blockName)
            elseEndBlock.statements += LLVMStatement.Jump(blockName)
            block.statements +=
                LLVMStatement.Branch(this[Type.Primitive.Bool], condition, elseStartName, thenStartName)
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.Loop.While) {
        val block = currentBlock
        newBlock()
        val conditionStartName = blockName
        val condition = this(ast.condition)
        val conditionEndBlock = currentBlock
        newBlock()
        val bodyStartName = blockName
        this(ast.statement)
        newBlock()
        block.statements += LLVMStatement.Jump(conditionStartName)
        conditionEndBlock.statements +=
            LLVMStatement.Branch(this[Type.Primitive.Bool], condition, blockName, bodyStartName)
        loopTarget[ast] = Pair(conditionStartName, blockName)
    }

    private operator fun invoke(ast: ASTNode.Statement.Loop.For) {
        if (ast.init != null) this(ast.init)
        val block = currentBlock
        newBlock()
        val conditionStartName = blockName
        val condition = this(ast.condition)
        val conditionEndBlock = currentBlock
        newBlock()
        val bodyStartName = blockName
        this(ast.statement)
        val bodyEndBlock = currentBlock
        newBlock()
        val stepStartName = blockName
        if (ast.step != null) this(ast.step)
        val stepEndBlock = currentBlock
        newBlock()
        block.statements += LLVMStatement.Jump(conditionStartName)
        bodyEndBlock.statements += LLVMStatement.Jump(stepStartName)
        stepEndBlock.statements += LLVMStatement.Jump(conditionStartName)
        conditionEndBlock.statements +=
            LLVMStatement.Branch(this[Type.Primitive.Bool], condition, blockName, bodyStartName)
        loopTarget[ast] = Pair(stepStartName, blockName)
    }

    operator fun invoke(ast: ASTNode.Declaration.Function): List<LLVMBlock> {
        localCount = ast.parameterList.size
        localBlocks.clear()
        newBlock()
        for ((index, variableDeclaration) in ast.parameterList.withIndex()) {
            val variable = variableDeclaration.actual
            val type = this[variable.type]
            val name = LLVMName.Local(variable.name)
            this += LLVMStatement.Alloca(name, type)
            this += LLVMStatement.Store(LLVMName.Local("$index"), type, name, LLVMType.Pointer(type))
            local[variable] = name
        }
        for (statement in ast.body.statements) this(statement)
        this += LLVMStatement.Ret(LLVMType.Void, null)
        return localBlocks.apply {
            forEach {
                if (it.statements.last() !is LLVMStatement.Terminating)
                    throw Exception("unterminated block found unexpectedly")
            }
        }
    }
}
