package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location

sealed class MxType(val size: Int) {
    abstract val functions: Map<String, MxFunction>

    sealed class Primitive(size: kotlin.Int) : MxType(size) {
        object Int : Primitive(4) {
            override val functions = mapOf<kotlin.String, MxFunction>()
            override fun toString() = "int"
        }

        object Bool : Primitive(1) {
            override val functions = mapOf<kotlin.String, MxFunction>()
            override fun toString() = "bool"
        }

        object String : Primitive(8) {
            override val functions = mapOf(
                "length" to MxFunction.Builtin.StringLength,
                "substring" to MxFunction.Builtin.StringSubstring,
                "parseInt" to MxFunction.Builtin.StringParseInt,
                "ord" to MxFunction.Builtin.StringOrd
            )

            override fun toString() = "string"
        }
    }

    object Null : MxType(8) {
        override val functions = mapOf<String, MxFunction>()
        override fun toString() = "null"
    }

    object Void : MxType(0) {
        override val functions = mapOf<String, MxFunction>()
        override fun toString() = "void"
    }

    object Unknown : MxType(0) {
        override val functions = mapOf<String, MxFunction>()
        override fun toString() = "<unknown type>"
    }

    class Class(val name: String, val def: ASTNode.Declaration.Class) : MxType(8) {
        class DuplicatedException(message: String) : Exception(message)

        val variables = mutableMapOf<String, MxVariable>()
        override val functions = mutableMapOf<String, MxFunction>()
        operator fun set(name: String, variable: MxVariable) {
            if (name in functions) throw DuplicatedException("\"$name\" is already defined as a function in \"$this\"")
            if (name in variables) throw DuplicatedException("\"$name\" is already defined as a variable in \"$this\"")
            if (name == this.name) throw DuplicatedException("\"$name\" has the same name of the class")
            variables[name] = variable
        }

        operator fun set(name: String, function: MxFunction) {
            if (name in functions) throw DuplicatedException("\"$name\" is already defined as a function in \"$this\"")
            if (name in variables) throw DuplicatedException("\"$name\" is already defined as a variable in \"$this\"")
            if (name == this.name) throw DuplicatedException("\"$name\" has the same name of the class")
            functions[name] = function
        }

        override fun toString() = name
    }

    class Array(
        val base: MxType
    ) : MxType(8) {
        override val functions = mapOf("size" to MxFunction.Builtin.ArraySize(this))
        override fun toString() = "$base[]"

        companion object {
            private val pool = mutableMapOf<Pair<MxType, Int>, Array>()
            fun get(base: MxType, dimension: Int, location: Location? = null): MxType = when (base) {
                Unknown -> Unknown
                Void -> Unknown.also {
                    ASTErrorRecorder.error(location!!, "void type doesn't have array type")
                }
                else -> if (base to dimension in pool) pool[base to dimension]!!
                else (if (dimension > 1) Array(get(base, dimension - 1)) else Array(base)).also {
                    pool[base to dimension] = it
                }
            }
        }
    }
}
