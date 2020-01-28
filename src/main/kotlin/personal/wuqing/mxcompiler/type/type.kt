package personal.wuqing.mxcompiler.type

sealed class Type {
    abstract val members: Map<String, Type>
}

sealed class PrimitiveType : Type()

object IntType : PrimitiveType() {
    override val members = mapOf<String, Type>() // TODO: builtin members of "int"
}

object BoolType : PrimitiveType() {
    override val members = mapOf<String, Type>() // TODO: builtin members of "bool"
}

object StringType : PrimitiveType() {
    override val members = mapOf<String, Type>() // TODO: builtin members of "string"
}

object NullType : PrimitiveType() {
    override val members = mapOf<String, Type>()
}

object VoidType: PrimitiveType() {
    override val members = mapOf<String, Type>()
}

object UnknownType: Type() {
    override val members = mapOf<String, Type>()
}

class ClassType(
    val name: String, override val members: Map<String, Type>
) : Type()

class ArrayType(
    val base: Type
) : Type() {
    override val members = mapOf<String, Type>() // TODO
}

class FunctionType(
    val returnType: Type, val parameterList: List<Type>
) : Type() {
    override val members = mapOf<String, Type>()
}
