package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder
import java.io.Serializable

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
        object Print : Builtin(Type.Void, null, "<builtin>print", listOf(Type.Primitive.String))
        object Println : Builtin(Type.Void, null, "<builtin>println", listOf(Type.Primitive.String))
        object PrintInt : Builtin(Type.Void, null, "<builtin>printInt", listOf(Type.Primitive.Int))
        object PrintlnInt : Builtin(Type.Void, null, "<builtin>printlnInt", listOf(Type.Primitive.Int))
        object GetString : Builtin(Type.Primitive.String, null, "<builtin>getString", listOf())
        object GetInt : Builtin(Type.Primitive.Int, null, "<builtin>.getInt", listOf())
        object ToString : Builtin(Type.Primitive.String, null, "<builtin>toString", listOf(Type.Primitive.Int))
        object StringLength : Builtin(Type.Primitive.Int, Type.Primitive.String, "<builtin>length", listOf())
        object StringParseInt : Builtin(Type.Primitive.Int, Type.Primitive.String, "<builtin>parseInt", listOf())
        class ArraySize(type: Type.Array) : Builtin(Type.Primitive.Int, type, "<builtin>size", listOf())
        class DefaultConstructor(type: Type.Class) : Builtin(type, type, "<default_constructor>", listOf())
        object StringOrd : Builtin(
            Type.Primitive.Int, Type.Primitive.String, "<builtin>ord", listOf(Type.Primitive.Int)
        )

        object StringSubstring : Builtin(
            Type.Primitive.String, Type.Primitive.String, "<builtin>substring",
            listOf(Type.Primitive.Int, Type.Primitive.Int)
        )
    }

}
