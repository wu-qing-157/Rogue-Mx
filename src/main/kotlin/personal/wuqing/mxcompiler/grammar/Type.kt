package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location

sealed class Type(val size: Int) {
    abstract val functions: Map<String, Function>

    sealed class Primitive(size: kotlin.Int) : Type(size) {
        object Int : Primitive(4) {
            override val functions = mapOf<kotlin.String, Function>()
            override fun toString() = "int"
        }

        object Bool : Primitive(1) {
            override val functions = mapOf<kotlin.String, Function>()
            override fun toString() = "bool"
        }

        object String : Primitive(8) {
            override val functions = mapOf(
                "length" to Function.Builtin.StringLength,
                "substring" to Function.Builtin.StringSubstring,
                "parseInt" to Function.Builtin.StringParseInt,
                "ord" to Function.Builtin.StringOrd
            )

            override fun toString() = "string"
        }
    }

    object Null : Type(8) {
        override val functions = mapOf<String, Function>()
        override fun toString() = "null"
    }

    object Void : Type(0) {
        override val functions = mapOf<String, Function>()
        override fun toString() = "void"
    }

    object Unknown : Type(0) {
        override val functions = mapOf<String, Function>()
        override fun toString() = "<unknown type>"
    }

    class Class(val name: String, val def: ASTNode.Declaration.Class) : Type(8) {
        class DuplicatedException(message: String) : Exception(message)

        val variables = mutableMapOf<String, Variable>()
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

    class Array(
        val base: Type
    ) : Type(8) {
        override val functions = mapOf("size" to Function.Builtin.ArraySize(this))
        override fun toString() = "$base[]"

        companion object {
            private val pool = mutableMapOf<Pair<Type, Int>, Array>()
            fun get(base: Type, dimension: Int, location: Location? = null): Type = when (base) {
                Unknown -> Unknown
                Void -> Unknown.also {
                    ASTErrorRecorder.error(location!!, "void type doesn't have array type")
                }
                else -> if (Pair(base, dimension) in pool) pool[base to dimension]!!
                else (if (dimension > 1) Array(get(base, dimension - 1)) else Array(base)).also {
                    pool[base to dimension] = it
                }
            }
        }
    }
}
