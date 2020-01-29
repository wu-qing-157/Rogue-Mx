package personal.wuqing.mxcompiler.frontend

import java.io.Serializable

sealed class Type : Serializable {
    abstract val variables: Map<String, Variable>
    abstract val functions: Map<FunctionDefinition, Function>
}

sealed class PrimitiveType : Type()

object IntType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>() // TODO: builtin member functions of "int"
}

object BoolType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>() // TODO: builtin member functions of "bool"
}

object StringType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>() // TODO: builtin member functions of "string"
}

object NullType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>()
}

object VoidType: PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>()
}

object UnknownType: Type() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>()
}

data class ClassType(
    val name: List<String>
) : Type() {
    override val variables = mutableMapOf<String, Variable>()
    override val functions = mutableMapOf<FunctionDefinition, Function>()
}

data class ArrayType(
    val base: Type
) : Type() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>() // TODO: builtin member functions of "array"
}
