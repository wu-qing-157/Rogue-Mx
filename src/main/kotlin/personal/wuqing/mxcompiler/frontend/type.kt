package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode
import java.io.Serializable

sealed class Type : Serializable {
    abstract val variables: Map<String, Variable>
}

sealed class PrimitiveType : Type()

object IntType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "int"
}

object BoolType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "bool"
}

object StringType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "string"
}

object NullType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "null"
}

object VoidType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "void"
}

object UnknownType : Type() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "unknown type"
}

data class ClassType(
    val name: String, val declaration: ASTNode.Declaration.Class
) : Type() {
    override val variables = mutableMapOf<String, Variable>()
    override fun toString() = name
}

data class ArrayType(
    val base: Type
) : Type() {
    override val variables = mapOf<String, Variable>()
    override fun toString() = "$base[]"
    override fun equals(other: Any?) = other is ArrayType && base == other.base
    override fun hashCode() = base.hashCode() - 0x1b6e3217 // random generated number
}
