package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode

class Variable constructor(val type: Type, val name: String, val declaration: ASTNode.Declaration.Variable) {
    init {
        declaration.init(this)
    }
}
