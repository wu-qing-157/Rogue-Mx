package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import java.io.Serializable

class Variable(val type: Type, val name: String, val declaration: ASTNode.Declaration.Variable) : Serializable {
    init {
        declaration.init(this)
    }
}
