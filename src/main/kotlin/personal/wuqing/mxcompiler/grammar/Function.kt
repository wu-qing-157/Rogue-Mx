package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder
import java.io.Serializable

private typealias Void_ = Type.Void
private typealias Int_ = Type.Primitive.Int
private typealias String_ = Type.Primitive.String
private typealias Bool_ = Type.Primitive.Bool

sealed class Function(
    val result: Type, open val base: Type?, val name: String, val parameters: List<Type>
) : Serializable {
    fun match(location: Location, call: List<Type>) =
        if (Type.Unknown in call || Type.Unknown in parameters) Type.Unknown
        else if (call.size != parameters.size || (call zip parameters).any { (c, p) -> c != Type.Null && c != p })
            Type.Unknown.also {
                SemanticErrorRecorder.error(
                    location, "cannot call function \"$this\" with \"(${call.joinToString()})\""
                )
            }
        else result

    class Top(
        result: Type, name: String, parameters: List<Type>, val def: ASTNode.Declaration.Function
    ) : Function(result, null, name, parameters) {
        init {
            def.init(this)
        }
    }

    class Member(
        result: Type, override val base: Type, name: String, parameters: List<Type>,
        val def: ASTNode.Declaration.Function
    ) : Function(result, base, name, parameters) {
        init {
            def.init(this)
        }
    }

    sealed class Builtin(
        result: Type, base: Type?, name: String, parameter: List<Type>
    ) : Function(result, base, name, parameter) {
        object Print : Builtin(Void_, null, "print", listOf(String_))
        object Println : Builtin(Void_, null, "println", listOf(String_))
        object PrintInt : Builtin(Void_, null, "printInt", listOf(Int_))
        object PrintlnInt : Builtin(Void_, null, "printlnInt", listOf(Int_))
        object GetString : Builtin(String_, null, "getString", listOf())
        object GetInt : Builtin(Int_, null, ".getInt", listOf())
        object ToString : Builtin(String_, null, "toString", listOf(Int_))
        object StringLength : Builtin(Int_, String_, "length", listOf())
        object StringParseInt : Builtin(Int_, String_, "parseInt", listOf())
        class ArraySize(type: Type.Array) : Builtin(Int_, type, "size", listOf())
        class DefaultConstructor(type: Type.Class) : Builtin(type, type, "__constructor__", listOf())
        object StringOrd : Builtin(Int_, String_, "ord", listOf(Int_))
        object StringSubstring : Builtin(String_, String_, "substring", listOf(Int_, Int_))
        object StringConcatenate : Builtin(String_, null, "__string__concatenate__", listOf(String_, String_))
        object StringEqual : Builtin(Bool_, null, "__string__equal__", listOf(String_, String_))
        object StringNeq : Builtin(Bool_, null, "__string__neq__", listOf(String_, String_))
        object StringLess : Builtin(Bool_, null, "__string__less__", listOf(String_, String_))
        object StringLeq : Builtin(Bool_, null, "__string__leq__", listOf(String_, String_))
        object StringGreater : Builtin(Bool_, null, "__string__greater__", listOf(String_, String_))
        object StringGeq : Builtin(Bool_, null, "__string__geq__", listOf(String_, String_))
    }
}
