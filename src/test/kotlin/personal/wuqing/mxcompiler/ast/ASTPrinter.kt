package personal.wuqing.mxcompiler.ast

object ASTPrinter {
    private val builder = StringBuilder()
    private fun write(s: String) = builder.append(s)
    private fun summary(root: ASTNode, depth: Int) {
        write(root.summary + "\n")
        val indent = (0..depth).joinToString("") { "    " }
        when (root) {
            is ASTNode.Program -> root.declarations.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is ASTNode.Declaration.Function -> {
                write(indent + "return: ")
                summary(root.result, depth + 1)
                root.parameterList.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
                write(indent + "body: ")
                summary(root.body, depth + 1)
            }
            is ASTNode.Declaration.Constructor -> {
                write(indent + "type: ")
                summary(root.type, depth + 1)
                root.parameterList.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
                write(indent + "body: ")
                summary(root.body, depth + 1)
            }
            is ASTNode.Declaration.Variable -> {
                write(indent + "type: ")
                summary(root.type, depth + 1)
                root.init?.let {
                    write(indent + "init: ")
                    summary(it, depth + 1)
                }
            }
            is ASTNode.Declaration.Class -> root.declarations.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is ASTNode.Statement.Empty -> {
            }
            is ASTNode.Statement.Block -> root.statements.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is ASTNode.Statement.Expression -> {
                write(indent)
                summary(root.expression, depth + 1)
            }
            is ASTNode.Statement.Variable -> root.variables.forEach {
                write(indent)
                summary(it, depth + 1)
            }
            is ASTNode.Statement.If -> {
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                write(indent + "then: ")
                summary(root.then, depth + 1)
                root.else_?.let {
                    write(indent + "else: ")
                    summary(it, depth + 1)
                }
            }
            is ASTNode.Statement.Loop.While -> {
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                write(indent + "loop: ")
                summary(root.statement, depth + 1)
            }
            is ASTNode.Statement.Loop.For -> {
                root.initVariable.forEach {
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
            is ASTNode.Statement.Continue, is ASTNode.Statement.Break -> {
            }
            is ASTNode.Statement.Return -> root.expression?.let {
                write(indent)
                summary(it, depth + 1)
            }
            is ASTNode.Expression.NewObject -> {
                write(indent + "type: ")
                summary(root.baseType, depth + 1)
                root.parameters.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
            }
            is ASTNode.Expression.NewArray -> {
                write(indent + "type: ")
                summary(root.baseType, depth + 1)
                root.length.forEach {
                    write(indent + "length: ")
                    summary(it, depth + 1)
                }
            }
            is ASTNode.Expression.MemberAccess -> {
                write(indent + "parent: ")
                summary(root.parent, depth + 1)
            }
            is ASTNode.Expression.MemberFunction -> {
                write(indent + "parent: ")
                summary(root.base, depth + 1)
                root.parameters.forEach {
                    write(indent + "p: ")
                    summary(it, depth + 1)
                }
            }
            is ASTNode.Expression.Function -> root.parameters.forEach {
                write(indent + "p: ")
                summary(it, depth + 1)
            }
            is ASTNode.Expression.Index -> {
                write(indent + "parent: ")
                summary(root.parent, depth + 1)
                write(indent + "child : ")
                summary(root.child, depth + 1)
            }
            is ASTNode.Expression.Suffix -> {
                write(indent + "operand: ")
                summary(root.operand, depth + 1)
            }
            is ASTNode.Expression.Prefix -> {
                write(indent + "operand: ")
                summary(root.operand, depth + 1)
            }
            is ASTNode.Expression.Binary -> {
                write(indent + "lhs: ")
                summary(root.lhs, depth + 1)
                write(indent + "rhs: ")
                summary(root.rhs, depth + 1)
            }
            is ASTNode.Expression.Ternary -> {
                write(indent + "cond: ")
                summary(root.condition, depth + 1)
                write(indent + "then: ")
                summary(root.then, depth + 1)
                write(indent + "else: ")
                summary(root.else_, depth + 1)
            }
            is ASTNode.Expression.Identifier, is ASTNode.Expression.This, is ASTNode.Expression.Constant, is ASTNode.Type -> {
            }
        }
    }

    fun summary(root: ASTNode): String {
        builder.clear()
        summary(root, 0)
        return builder.toString()
    }
}
