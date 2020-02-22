package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode
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
    }

    private fun initClasses(root: ASTNode.Program) {
        root.declarations.filterIsInstance<ASTNode.Declaration.Class>().forEach {
            ClassTable[it.name] = it.clazz
        }
    }

    private fun initFunctions(root: ASTNode.Program) {
        root.declarations.filterIsInstance<ASTNode.Declaration.Class>().forEach { clazz ->
            clazz.declarations.forEach {
                when (it) {
                    is ASTNode.Declaration.Constructor ->
                        FunctionTable[FunctionDefinition(
                            clazz.clazz, "<constructor>",
                            it.parameterList.map { p -> p.type.type })] = Function(it.type.type, it.body)
                    is ASTNode.Declaration.Function ->
                        FunctionTable[FunctionDefinition(
                            clazz.clazz, it.name,
                            it.parameterList.map { p -> p.type.type })] = Function(it.returnType.type, it.body)
                }
            }
        }
        root.declarations.filterIsInstance<ASTNode.Declaration.Function>().forEach {
            FunctionTable[FunctionDefinition(
                null, it.name,
                it.parameterList.map { p -> p.type.type })] = Function(it.returnType.type, it.body)
        }
    }

    private fun visit(node: ASTNode.Declaration.Class) {
        SymbolTable.new(node.clazz)
        node.declarations.forEach {
            when (it) {
                is ASTNode.Declaration.Function -> visit(it)
                is ASTNode.Declaration.Variable -> {
                    visit(it)
                    node.clazz.variables[it.name] = Variable(it.type.type, it)
                }
            }
        }
        SymbolTable.drop()
    }

    private fun visit(node: ASTNode.Declaration.Function) {
        SymbolTable.new()
        node.parameterList.forEach { visit(it) }
        visit(node.body)
        SymbolTable.drop()
    }

    private fun visit(node: ASTNode.Declaration.Variable) {
        if (node.init != null && node.init.type != UnknownType && node.type.type != UnknownType && node.init.type != node.type.type)
            SemanticErrorRecorder.error(
                node.location,
                "cannot initialize \"${node.name}\" of type \"${node.type.type}\" with \"${node.init.type}\""
            )
        else
            try {
                VariableTable[node.name] = Variable(node.type.type, node)
            } catch (e: SymbolTable.DuplicatedException) {
                SemanticErrorRecorder.error(node.location, "variable \"${node.name}\" has already been defined")
            }
    }

    private fun visit(node: ASTNode.Statement) {
        when (node) {
            is ASTNode.Statement.Empty, is ASTNode.Statement.Continue, is ASTNode.Statement.Break -> {
            }
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
                visit(node.then)
                node.else_?.let { visit(it) }
            }
            is ASTNode.Statement.While -> {
                if (node.condition.type != BoolType && node.condition.type != UnknownType) SemanticErrorRecorder.error(
                    node.location,
                    "condition must have type \"bool\", but \"${node.condition.type}\" found"
                )
                visit(node.statement)
            }
            is ASTNode.Statement.For -> {
                SymbolTable.new()
                node.initVariable.forEach { visit(it) }
                node.initExpression?.type
                if (node.condition.type != BoolType && node.condition.type != UnknownType) SemanticErrorRecorder.error(
                    node.location,
                    "condition must have type \"bool\", but \"${node.condition.type}\" found"
                )
                node.step?.type
                visit(node.statement)
                SymbolTable.drop()
            }
            is ASTNode.Statement.Return -> node.expression?.type
        }
    }
}
