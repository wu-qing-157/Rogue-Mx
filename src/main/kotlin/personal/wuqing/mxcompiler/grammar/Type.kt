package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location
import java.io.Serializable

sealed class Type : Serializable {
    abstract val variables: Map<String, Variable>
    abstract val functions: Map<String, Function>

    sealed class Primitive : Type() {
        object Int : Primitive() {
            override val variables = mapOf<kotlin.String, Variable>()
            override val functions = mapOf<kotlin.String, Function>()
            override fun toString() = "int"
        }

        object Bool : Primitive() {
            override val variables = mapOf<kotlin.String, Variable>()
            override val functions = mapOf<kotlin.String, Function>()
            override fun toString() = "bool"
        }

        object String : Primitive() {
            override val variables = mapOf<kotlin.String, Variable>()
            override val functions = mapOf(
                "length" to Function.Builtin.StringLength,
                "substring" to Function.Builtin.StringSubstring,
                "parseInt" to Function.Builtin.StringParseInt,
                "ord" to Function.Builtin.StringOrd
            )

            override fun toString() = "string"
        }
    }

    object Null : Type() {
        override val variables = mapOf<kotlin.String, Variable>()
        override val functions = mapOf<kotlin.String, Function>()
        override fun toString() = "null"
    }

    object Void : Type() {
        override val variables = mapOf<kotlin.String, Variable>()
        override val functions = mapOf<kotlin.String, Function>()
        override fun toString() = "void"
    }

    object Unknown : Type() {
        override val variables = mapOf<kotlin.String, Variable>()
        override val functions = mapOf<kotlin.String, Function>()
        override fun toString() = "<unknown type>"
    }

    data class Class(
        val name: String, val declaration: ASTNode.Declaration.Class
    ) : Type() {
        class DuplicatedException(message: String) : Exception(message)

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

    data class Array(
        val base: Type
    ) : Type() {
        override val variables = mapOf<kotlin.String, Variable>()
        override val functions = mapOf("size" to Function.Builtin.ArraySize)
        override fun toString() = "$base[]"
        override fun equals(other: Any?) = other is Array && base == other.base
        override fun hashCode() = base.hashCode() - 0x1b6e3217 // random generated number

        companion object {
            private val pool = mutableMapOf<Pair<Type, Int>, Array>()
            fun get(base: Type, dimension: Int, location: Location? = null): Type =
                when (base) {
                    Unknown -> Unknown
                    Void -> Unknown.also {
                        ASTErrorRecorder.error(location!!, "void type doesn't have array type")
                    }
                    else ->
                        if (Pair(base, dimension) in pool) pool[Pair(base, dimension)]!!
                        else (if (dimension > 1) Array(get(base, dimension - 1)) else Array(base)).also {
                            pool[Pair(base, dimension)] = it
                        }
                }
        }
    }
}
