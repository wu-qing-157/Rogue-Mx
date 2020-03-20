package personal.wuqing.rogue.grammar

import personal.wuqing.rogue.ast.ASTNode

class MxVariable(val type: MxType, val name: String, val declaration: ASTNode.Declaration.Variable) {
    init {
        declaration.init(this)
    }
}
