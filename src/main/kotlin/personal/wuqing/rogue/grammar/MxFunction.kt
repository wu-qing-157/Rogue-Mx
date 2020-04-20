package personal.wuqing.rogue.grammar

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.utils.Location
import personal.wuqing.rogue.utils.SemanticErrorRecorder

private typealias Void_ = MxType.Void
private typealias Int_ = MxType.Primitive.Int
private typealias String_ = MxType.Primitive.String

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
        object StringOrd : Builtin(Int_, String_, "ord", listOf(Int_))
        object StringSubstring : Builtin(String_, String_, "substring", listOf(Int_, Int_))
    }
}
