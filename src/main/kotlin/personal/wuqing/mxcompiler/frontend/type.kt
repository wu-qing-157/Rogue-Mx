package personal.wuqing.mxcompiler.frontend

import personal.wuqing.mxcompiler.ast.ASTNode
import java.io.Serializable

sealed class Type : Serializable {
    abstract val variables: Map<String, Variable>
    abstract val functions: Map<FunctionDefinition, Function>
}

sealed class PrimitiveType : Type()

object IntType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions: Map<FunctionDefinition, Function> get() = TODO("builtin member functions of \"int\"")
    override fun toString() = "int"
}

object BoolType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions: Map<FunctionDefinition, Function> get() = TODO("builtin member functions of \"bool\"")
    override fun toString() = "bool"
}

object StringType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions: Map<FunctionDefinition, Function> get() = TODO("builtin member functions of \"string\"")
    override fun toString() = "string"
}

object NullType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>()
    override fun toString() = "null"
}

object VoidType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>()
    override fun toString() = "void"
}

object UnknownType : Type() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<FunctionDefinition, Function>()
    override fun toString() = "unknown type"
}

data class ClassType(
    val name: String, val declaration: ASTNode.Declaration.Class
) : Type() {
    override val variables = mutableMapOf<String, Variable>()
    override val functions = mutableMapOf<FunctionDefinition, Function>()
    override fun toString() = name
}

data class ArrayType(
    val base: Type
) : Type() {
    override val variables = mapOf<String, Variable>()
    override val functions: Map<FunctionDefinition, Function> get() = TODO("builtin member functions of \"array\"")
    override fun toString() = "$base[]"
    override fun equals(other: Any?) = other is ArrayType && base == other.base
    override fun hashCode() = base.hashCode() - 0x1b6e3217 // random generated number
}

/*
fun initClasses(root: ProgramNode) {
    root.declarations.filterIsInstance<ClassDeclarationNode>().forEach { Scope.TOP[it.name] = ClassType(it.name, it) }
}
*/
