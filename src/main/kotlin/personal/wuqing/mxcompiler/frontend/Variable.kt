package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode
import java.io.Serializable

data class Variable(val type: Type, val declaration: ASTNode.Declaration.Variable) : Serializable
