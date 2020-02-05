package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.BlockNode

data class FunctionDefinition(val base: Type?, val name: String, val parameterList: List<Type>)

class Function(val definition: FunctionDefinition, val body: BlockNode) {
}
