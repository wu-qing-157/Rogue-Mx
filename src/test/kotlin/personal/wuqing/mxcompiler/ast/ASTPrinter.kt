package personal.wuqing.mxcompiler.ast

object ASTPrinter {
    private val builder = StringBuilder()
    private fun write(s: String) = builder.append(s)
    private fun summary(root: ASTNode, depth: Int) {
        write(root.summary + "\n")
        val indent = (0..depth).joinToString("") {"    "}
        when (root) {
            is ProgramNode -> root.declarations.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is ParameterNode -> {
                write(indent + "type: ")
                summary(root.type, depth + 1)
            }
            is FunctionDeclarationNode -> {
                write(indent + "return: ")
                summary(root.returnType, depth + 1)
                root.parameterList.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
                write(indent + "body: ")
                summary(root.body, depth + 1)
            }
            is ConstructorDeclarationNode -> {
                write(indent + "type: ")
                summary(root.type, depth + 1)
                root.parameterList.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
                write(indent + "body: ")
                summary(root.body, depth + 1)
            }
            is VariableDeclarationNode -> {
                write(indent + "type: ")
                summary(root.type, depth + 1)
                root.init?.let {
                    write(indent + "init: ")
                    summary(it, depth + 1)
                }
            }
            is ClassDeclarationNode -> {
                root.variables.forEach {
                    write(indent)
                    summary(it, depth + 1)
                }
                root.functions.forEach {
                    write(indent)
                    summary(it, depth + 1)
                }
                root.constructors.forEach {
                    write(indent)
                    summary(it, depth + 1)
                }
            }
            is EmptyStatementNode -> {}
            is BlockNode -> root.statements.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is ExpressionStatementNode -> {
                write(indent)
                summary(root.expression, depth + 1)
            }
            is VariableDeclarationStatementNode -> root.variables.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is IfNode -> {
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                write(indent + "then: ")
                summary(root.thenStatement, depth + 1)
                root.elseStatement?.let {
                    write(indent + "else: ")
                    summary(it, depth + 1)
                }
            }
            is WhileNode -> {
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                write(indent + "loop: ")
                summary(root.statement, depth + 1)
            }
            is ForNode -> {
                root.initVariableDeclaration.forEach {
                    write(indent + "init: ")
                    summary(it, depth + 1)
                }
                root.initExpression?.let {
                    write(indent + "init: ")
                    summary(it, depth + 1)
                }
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                root.step?.let {
                    write(indent + "step: ")
                    summary(it, depth + 1)
                }
                write(indent + "loop: ")
                summary(root.statement, depth + 1)
            }
            is ContinueNode, is BreakNode -> {}
            is ReturnNode -> root.expression?.let {
                write(indent)
                summary(it, depth + 1)
            }
            is NewObjectNode -> {
                write(indent + "type: ")
                summary(root.baseType, depth + 1)
                root.parameters.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
            }
            is NewArrayNode -> {
                write(indent + "type: ")
                summary(root.baseType, depth + 1)
                root.length.forEach {
                    write(indent + "length: ")
                    summary(it, depth + 1)
                }
            }
            is MemberAccessNode -> {
                write(indent + "parent: ")
                summary(root.parent, depth + 1)
            }
            is MemberFunctionCallNode -> {
                write(indent + "parent: ")
                summary(root.parent, depth + 1)
                root.parameters.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
            }
            is FunctionCallNode -> root.parameters.forEach {
                write(indent + "p: ")
                summary(it, depth + 1)
            }
            is IndexAccessNode -> {
                write(indent + "parent: ")
                summary(root.parent, depth + 1)
                write(indent + "child : ")
                summary(root.child, depth + 1)
            }
            is SuffixUnaryNode -> {
                write(indent + "operand: ")
                summary(root.operand, depth + 1)
            }
            is PrefixUnaryNode -> {
                write(indent + "operand: ")
                summary(root.operand, depth + 1)
            }
            is BinaryNode -> {
                write(indent + "lhs: ")
                summary(root.lhs, depth + 1)
                write(indent + "rhs: ")
                summary(root.rhs, depth + 1)
            }
            is TernaryNode -> {
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                write(indent + "then: ")
                summary(root.thenExpression, depth + 1)
                write(indent + "else: ")
                summary(root.elseExpression, depth + 1)
            }
            is IdentifierExpressionNode, is ThisExpressionNode, is ConstantNode, is TypeNode -> {}
        }
    }
    fun summary(root: ASTNode): String {
        builder.clear()
        summary(root, 0)
        return builder.toString()
    }
}
