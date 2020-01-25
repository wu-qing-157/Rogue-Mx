package personal.wuqing.mxcompiler.type

sealed class Type

sealed class PrimitiveType : Type()

object IntType : PrimitiveType()

object BoolType : PrimitiveType()

object StringType : PrimitiveType()

object NullType : PrimitiveType()

object VoidType: PrimitiveType()

class ClassType(
    val name: String
) : Type()

class ArrayType(
    val base: Type
) : Type()
