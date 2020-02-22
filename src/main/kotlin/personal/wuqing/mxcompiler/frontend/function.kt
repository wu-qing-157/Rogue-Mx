package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode

data class FunctionDefinition(val base: Type?, val name: String, val parameterList: List<Type>)

data class Function(val returnType: Type, val body: ASTNode.Statement.Block)

/*
fun initFunctions(root: ASTNode.Program) {
    root.declarations.filterIsInstance<ASTNode.Declaration.Function>().forEach {
        Scope.TOP[FunctionDefinition(null, it.name, it.parameterList.map { t -> t.type.type })] =
            Function(it.returnType.type, it.body)
    }
    root.declarations.filterIsInstance<ASTNode.Declaration.Class>().forEach { clazz ->
        clazz.functions.forEach {
            Scope.TOP[FunctionDefinition(clazz.type, it.name, it.parameterList.map { t -> t.type.type })] =
                Function(it.returnType.type, it.body)
        }
    }
}
*/
