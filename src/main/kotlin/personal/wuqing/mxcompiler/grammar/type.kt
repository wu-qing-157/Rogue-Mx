package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location
import java.io.Serializable

sealed class Type : Serializable {
    abstract val variables: Map<String, Variable>
    abstract val functions: Map<String, Function>
}

sealed class PrimitiveType : Type()

object IntType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<String, Function>()
    override fun toString() = "int"
}

object BoolType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<String, Function>()
    override fun toString() = "bool"
}

object StringType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf(
        "length" to BuiltinFunction.StringLength,
        "substring" to BuiltinFunction.StringSubstring,
        "parseInt" to BuiltinFunction.StringParseInt,
        "ord" to BuiltinFunction.StringOrd
    )

    override fun toString() = "string"
}

object NullType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<String, Function>()
    override fun toString() = "null"
}

object VoidType : PrimitiveType() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<String, Function>()
    override fun toString() = "void"
}

object UnknownType : Type() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf<String, Function>()
    override fun toString() = "<unknown type>"
}

data class ClassType(
    val name: String, val declaration: ASTNode.Declaration.Class
) : Type() {
    class DuplicatedException(val info: String) : Exception()

    override val variables = mutableMapOf<String, Variable>()
    override val functions = mutableMapOf<String, Function>()
    operator fun set(name: String, variable: Variable) {
        if (name in functions) throw DuplicatedException("\"$name\" is already defined as a function in \"$this\"")
        if (name in variables) throw DuplicatedException("\"$name\" is already defined as a variable in \"$this\"")
        if (name == this.name) throw DuplicatedException("\"$name\" has the same name of the class")
        variables[name] = variable
    }

    operator fun set(name: String, function: Function) {
        if (name in functions) throw DuplicatedException("\"$name\" is already defined as a function in \"$this\"")
        if (name in variables) throw DuplicatedException("\"$name\" is already defined as a variable in \"$this\"")
        if (name == this.name) throw DuplicatedException("\"$name\" has the same name of the class")
        functions[name] = function
    }

    override fun toString() = name
}

data class ArrayType(
    val base: Type
) : Type() {
    override val variables = mapOf<String, Variable>()
    override val functions = mapOf("size" to BuiltinFunction.ArraySize)
    override fun toString() = "$base[]"
    override fun equals(other: Any?) = other is ArrayType && base == other.base
    override fun hashCode() = base.hashCode() - 0x1b6e3217 // random generated number

    companion object {
        private val pool = mutableMapOf<Pair<Type, Int>, ArrayType>()
        fun getArrayType(base: Type, dimension: Int, location: Location? = null): Type =
            when (base) {
                UnknownType -> UnknownType
                VoidType -> UnknownType.also {
                    ASTErrorRecorder.error(location!!, "void type doesn't have array type")
                }
                else ->
                    if (Pair(base, dimension) in pool)
                        pool[Pair(base, dimension)]!!
                    else
                        (if (dimension > 1) ArrayType(getArrayType(base, dimension - 1))
                        else ArrayType(base)).also {
                            pool[Pair(base, dimension)] = it
                        }
            }
    }
}
