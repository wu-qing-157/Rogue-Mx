package personal.wuqing.mxcompiler.semantic

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.grammar.Function
import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.grammar.Variable
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder

object SemanticMain {
    operator fun invoke(root: ASTNode.Program) {
        initClasses(root)
        initBuiltinFunction()
        initFunctions(root)
        initClassMembers(root)
        root.declarations.forEach { visit(it) }
        checkMain(root.location)
    }

    private fun initClasses(root: ASTNode.Program) =
        root.declarations.filterIsInstance<ASTNode.Declaration.Class>().forEach {
            try {
                ClassTable[it.name] = it.clazz
            } catch (e: SymbolTableException) {
                SemanticErrorRecorder.error(root.location, e.toString())
            }
        }

    private fun initBuiltinFunction() = mapOf(
        "print" to Function.Builtin.Print,
        "println" to Function.Builtin.Println,
        "printInt" to Function.Builtin.PrintInt,
        "printlnInt" to Function.Builtin.PrintlnInt,
        "getString" to Function.Builtin.GetString,
        "getInt" to Function.Builtin.GetInt,
        "toString" to Function.Builtin.ToString
    ).forEach { (def, func) -> FunctionTable[def] = func }

    private fun initFunctions(root: ASTNode.Program) =
        root.declarations.filterIsInstance<ASTNode.Declaration.Function>().forEach {
            FunctionTable[it.name] =
                Function(it.result.type, it.parameterList.map { p -> p.type.type }, it.body)
        }

    private fun initClassMembers(root: ASTNode.Program) =
        root.declarations.filterIsInstance<ASTNode.Declaration.Class>().forEach { clazz ->
            clazz.declarations.forEach {
                try {
                    when (it) {
                        is ASTNode.Declaration.Function -> {
                            if (it is ASTNode.Declaration.Constructor && it.result.type != clazz.clazz)
                                SemanticErrorRecorder.error(
                                    it.location,
                                    "constructor of \"${it.result.type}\" found in definition of \"${clazz.clazz}\""
                                )
                            clazz.clazz[it.name] =
                                Function(
                                    it.result.type, it.parameterList.map { p -> p.type.type },
                                    it.body
                                )
                        }
                        is ASTNode.Declaration.Variable ->
                            clazz.clazz[it.name] = Variable(it.type.type, it)
                    }
                } catch (e: Type.Class.DuplicatedException) {
                    SemanticErrorRecorder.error(it.location, e.info)
                }
            }
            if (clazz.declarations.none { it is ASTNode.Declaration.Constructor })
                try {
                    clazz.clazz["<constructor>"] =
                        Function.Builtin.DefaultConstructor(clazz.clazz)
                } catch (e: Type.Class.DuplicatedException) {
                    SemanticErrorRecorder.error(clazz.location, e.info)
                }
        }

    private fun visit(node: ASTNode.Declaration) = when (node) {
        is ASTNode.Declaration.Function -> visit(node)
        is ASTNode.Declaration.Variable -> visit(node)
        is ASTNode.Declaration.Class -> visit(node)
    }

    private fun visit(node: ASTNode.Declaration.Class) {
        SymbolTable.new(node.clazz)
        node.declarations.forEach {
            when (it) {
                is ASTNode.Declaration.Function -> visit(it)
                is ASTNode.Declaration.Constructor -> visit(it)
                is ASTNode.Declaration.Variable -> visit(it)
            }
        }
        SymbolTable.drop()
    }

    private fun visit(node: ASTNode.Declaration.Function) {
        SymbolTable.new()
        SymbolTable.newFunction(node)
        node.parameterList.forEach { visit(it) }
        visit(node.body)
        SymbolTable.dropFunction()
        SymbolTable.drop()
    }

    private fun visit(node: ASTNode.Declaration.Constructor) {
        SymbolTable.new()
        SymbolTable.newFunction(node)
        node.parameterList.forEach { visit(it) }
        visit(node.body)
    }

    private fun visit(node: ASTNode.Declaration.Variable) {
        if (node.type.type == Type.Void)
            SemanticErrorRecorder.error(node.location, "cannot declare variable of void type")
        if (node.init != null && node.init.type != Type.Unknown && node.type.type != Type.Unknown
            && node.init.type != node.type.type && node.init.type != Type.Null
        ) SemanticErrorRecorder.error(
            node.location, "cannot initialize \"${node.name}\" of type \"${node.type.type}\" with \"${node.init.type}\""
        )
        else
            try {
                VariableTable[node.name] = Variable(node.type.type, node)
            } catch (e: SymbolTableException) {
                SemanticErrorRecorder.error(node.location, e.toString())
            }
    }

    private fun visit(node: ASTNode.Statement) {
        when (node) {
            is ASTNode.Statement.Block -> {
                SymbolTable.new()
                node.statements.forEach { visit(it) }
                SymbolTable.drop()
            }
            is ASTNode.Statement.Expression -> node.expression.type
            is ASTNode.Statement.Variable -> node.variables.forEach { visit(it) }
            is ASTNode.Statement.If -> {
                if (node.condition.type != Type.Primitive.Bool && node.condition.type != Type.Unknown) SemanticErrorRecorder.error(
                    node.location, "condition must have type \"bool\", but \"${node.condition.type}\" found"
                )
                SymbolTable.new()
                visit(node.then)
                SymbolTable.drop()
                node.else_?.let {
                    SymbolTable.new()
                    visit(it)
                    SymbolTable.drop()
                }
            }
            is ASTNode.Statement.Loop.While -> {
                if (node.condition.type != Type.Primitive.Bool && node.condition.type != Type.Unknown) SemanticErrorRecorder.error(
                    node.location, "condition must have type \"bool\", but \"${node.condition.type}\" found"
                )
                SymbolTable.new()
                SymbolTable.newLoop(node)
                visit(node.statement)
                SymbolTable.drop()
                SymbolTable.dropLoop()
            }
            is ASTNode.Statement.Loop.For -> {
                SymbolTable.new()
                node.initVariable.forEach { visit(it) }
                node.initExpression?.type
                if (node.condition.type != Type.Primitive.Bool && node.condition.type != Type.Unknown) SemanticErrorRecorder.error(
                    node.location, "condition must have type \"bool\", but \"${node.condition.type}\" found"
                )
                node.step?.type
                SymbolTable.newLoop(node)
                visit(node.statement)
                SymbolTable.dropLoop()
                SymbolTable.drop()
            }
            is ASTNode.Statement.Continue -> SymbolTable.loop
                ?: SemanticErrorRecorder.error(node.location, "\"continue\" found without loop")
            is ASTNode.Statement.Break -> SymbolTable.loop
                ?: SemanticErrorRecorder.error(node.location, "\"break\" found without loop")
            is ASTNode.Statement.Return -> {
                if (SymbolTable.returnType == null)
                    SemanticErrorRecorder.error(node.location, "\"return\" found without function")
                else if (SymbolTable.returnType!! != Type.Unknown
                    && node.expression?.type != Type.Unknown
                    && (if (SymbolTable.returnType == Type.Void) node.expression != null else node.expression == null
                            || (node.expression.type != Type.Null && node.expression.type != SymbolTable.returnType))
                )
                    SemanticErrorRecorder.error(
                        node.location, "\"${node.expression?.type ?: Type.Void}\" returned, " +
                                "but \"${SymbolTable.returnType}\" expected"
                    )
            }
        }
    }

    private fun checkMain(location: Location) {
        try {
            if (FunctionTable["main"].match(location, listOf()) !in listOf(Type.Unknown, Type.Primitive.Int))
                SemanticErrorRecorder.error(location, "\"main()\" must return \"int\", but \"$this\" found")
        } catch (e: SymbolTableException) {
            SemanticErrorRecorder.error(location, "cannot find function \"main()\"")
        }
    }

    fun reportSuccess() = SemanticErrorRecorder.info("semantic passed successful")
}