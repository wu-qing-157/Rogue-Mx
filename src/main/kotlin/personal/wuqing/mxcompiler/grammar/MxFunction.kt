package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder

private typealias Void_ = MxType.Void
private typealias Int_ = MxType.Primitive.Int
private typealias String_ = MxType.Primitive.String
private typealias Bool_ = MxType.Primitive.Bool

sealed class MxFunction(val result: MxType, open val base: MxType?, val name: String, val parameters: List<MxType>) {
    fun match(location: Location, call: List<MxType>) =
        if (MxType.Unknown in call || MxType.Unknown in parameters) MxType.Unknown
        else if (call.size != parameters.size || (call zip parameters).any { (c, p) -> c != MxType.Null && c != p })
            MxType.Unknown.also {
                SemanticErrorRecorder.error(
                    location, "cannot call function \"$this\" with \"(${call.joinToString()})\""
                )
            }
        else result

    class Top(
        result: MxType, name: String, parameters: List<MxType>, val def: ASTNode.Declaration.Function
    ) : MxFunction(result, null, name, parameters) {
        init {
            def.init(this)
        }
    }

    class Member(
        result: MxType, override val base: MxType, name: String, parameters: List<MxType>,
        val def: ASTNode.Declaration.Function
    ) : MxFunction(result, base, name, parameters) {
        init {
            def.init(this)
        }
    }

    sealed class Builtin(
        result: MxType, base: MxType?, name: String, parameter: List<MxType>
    ) : MxFunction(result, base, name, parameter) {
        object Print : Builtin(Void_, null, "print", listOf(String_))
        object Println : Builtin(Void_, null, "println", listOf(String_))
        object PrintInt : Builtin(Void_, null, "printInt", listOf(Int_))
        object PrintlnInt : Builtin(Void_, null, "printlnInt", listOf(Int_))
        object GetString : Builtin(String_, null, "getString", listOf())
        object GetInt : Builtin(Int_, null, ".getInt", listOf())
        object ToString : Builtin(String_, null, "toString", listOf(Int_))
        object StringLength : Builtin(Int_, String_, "length", listOf())
        object StringParseInt : Builtin(Int_, String_, "parseInt", listOf())
        class ArraySize(type: MxType.Array) : Builtin(Int_, type, "size", listOf()) {
            override fun equals(other: Any?) = other is ArraySize
            override fun hashCode(): Int {
                return javaClass.hashCode()
            }
        }

        class DefaultConstructor(type: MxType.Class) : Builtin(type, type, "__constructor__", listOf())
        object StringLiteral : Builtin(String_, null, "__string__literal__", listOf(String_, Int_))
        object StringOrd : Builtin(Int_, String_, "ord", listOf(Int_))
        object StringSubstring : Builtin(String_, String_, "substring", listOf(Int_, Int_))
        object Malloc : Builtin(String_, null, "__malloc__", listOf(Int_))
        object MallocArray : Builtin(String_, null, "__malloc__array__", listOf(Int_, Int_))
        object StringConcatenate : Builtin(String_, null, "__string__concatenate__", listOf(String_, String_))
        object StringEqual : Builtin(Bool_, null, "__string__equal__", listOf(String_, String_))
        object StringNeq : Builtin(Bool_, null, "__string__neq__", listOf(String_, String_))
        object StringLess : Builtin(Bool_, null, "__string__less__", listOf(String_, String_))
        object StringLeq : Builtin(Bool_, null, "__string__leq__", listOf(String_, String_))
        object StringGreater : Builtin(Bool_, null, "__string__greater__", listOf(String_, String_))
        object StringGeq : Builtin(Bool_, null, "__string__geq__", listOf(String_, String_))
    }
}
