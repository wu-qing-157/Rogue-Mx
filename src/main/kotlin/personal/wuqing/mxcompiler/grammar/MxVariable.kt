package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode

class MxVariable(val type: MxType, val name: String, val declaration: ASTNode.Declaration.Variable) {
    init {
        declaration.init(this)
    }
}
