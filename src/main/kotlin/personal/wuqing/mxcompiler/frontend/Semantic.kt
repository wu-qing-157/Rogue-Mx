package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder

object Semantic {
    fun run(root: ASTNode.Program) {
        initClasses(root)
        initFunctions(root)
        root.declarations.forEach {
            when (it) {
                is ASTNode.Declaration.Class -> visit(it)
                is ASTNode.Declaration.Function -> visit(it)
                is ASTNode.Declaration.Variable -> visit(it)
            }
        }
        try {
            if (FunctionTable["main"].match(root.location, listOf()) !in listOf(UnknownType, IntType))
                ASTErrorRecorder.error(root.location, "\"main()\" must return \"int\", but \"$this\" found")
        } catch (e: SymbolTableException) {
            ASTErrorRecorder.error(root.location, "cannot find function \"main()\"")
        }
    }

    private fun initClasses(root: ASTNode.Program) {
        root.declarations.filterIsInstance<ASTNode.Declaration.Class>().forEach {
            try {
                ClassTable[it.name] = it.clazz
            } catch (e: SymbolTableException) {
                SemanticErrorRecorder.error(root.location, e.toString())
            }
        }
    }

    private fun initFunctions(root: ASTNode.Program) {
        BuiltinFunction.initBuiltinFunction()
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
                                Function(it.result.type, it.parameterList.map { p -> p.type.type }, it.body)
                        }
                        is ASTNode.Declaration.Variable ->
                            clazz.clazz[it.name] = Variable(it.type.type, it)
                    }
                } catch (e: ClassType.DuplicatedException) {
                    SemanticErrorRecorder.error(it.location, e.info)
                }
            }
            if (clazz.declarations.none { it is ASTNode.Declaration.Constructor })
                try {
                    clazz.clazz["<constructor>"] = BuiltinFunction.DefaultConstructor(clazz.clazz)
                } catch (e: ClassType.DuplicatedException) {
                    SemanticErrorRecorder.error(clazz.location, e.info)
                }
        }
        root.declarations.filterIsInstance<ASTNode.Declaration.Function>().forEach {
            FunctionTable[it.name] = Function(it.result.type, it.parameterList.map { p -> p.type.type }, it.body)
        }
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
        if (node.type.type == VoidType)
            SemanticErrorRecorder.error(node.location, "cannot declare variable of void type")
        if (node.init != null && node.init.type != UnknownType && node.type.type != UnknownType
            && node.init.type != node.type.type && node.init.type != NullType
        ) SemanticErrorRecorder.error(
            node.location,
            "cannot initialize \"${node.name}\" of type \"${node.type.type}\" with \"${node.init.type}\""
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
                if (node.condition.type != BoolType && node.condition.type != UnknownType) SemanticErrorRecorder.error(
                    node.location,
                    "condition must have type \"bool\", but \"${node.condition.type}\" found"
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
                if (node.condition.type != BoolType && node.condition.type != UnknownType) SemanticErrorRecorder.error(
                    node.location,
                    "condition must have type \"bool\", but \"${node.condition.type}\" found"
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
                if (node.condition.type != BoolType && node.condition.type != UnknownType) SemanticErrorRecorder.error(
                    node.location,
                    "condition must have type \"bool\", but \"${node.condition.type}\" found"
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
                else if (SymbolTable.returnType!! != UnknownType
                    && node.expression?.type != UnknownType
                    && (
                            if (SymbolTable.returnType == VoidType) node.expression != null
                            else node.expression == null
                                    || (node.expression.type != NullType && node.expression.type != SymbolTable.returnType))
                )
                    SemanticErrorRecorder.error(
                        node.location,
                        "\"${node.expression?.type ?: VoidType}\" returned, " +
                                "but \"${SymbolTable.returnType}\" expected"
                    )
            }
        }
    }
}
