package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode

data class FunctionDefinition(val base: Type?, val name: String, val parameterList: List<Type>) {
    override fun toString() = "${base?.let { "$base." } ?: ""}$name(${parameterList.joinToString()})"
}

data class Function(val returnType: Type, val body: ASTNode.Statement.Block)
