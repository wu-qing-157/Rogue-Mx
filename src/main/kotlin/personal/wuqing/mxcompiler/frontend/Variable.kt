package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode

data class VariableName(val name: String)

class Variable(val type: Type, val declaration: ASTNode.Declaration.Variable)
