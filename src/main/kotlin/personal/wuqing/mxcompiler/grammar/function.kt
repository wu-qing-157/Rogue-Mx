package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder
import java.io.Serializable

open class Function(val result: Type, val parameter: List<Type>, val body: ASTNode.Statement.Block?) : Serializable {
    fun match(location: Location, call: List<Type>) =
        if (Type.Unknown in call || Type.Unknown in parameter) Type.Unknown
        else if (call.size != parameter.size || (call zip parameter).any { (c, p) -> c != Type.Null && c != p })
            Type.Unknown.also {
                SemanticErrorRecorder.error(
                    location, "cannot call function \"$this\" with \"(${call.joinToString()})\""
                )
            }
        else result

    sealed class Builtin(result: Type, parameter: List<Type>) : Function(result, parameter, null) {
        object Print : Builtin(Type.Void, listOf(Type.Primitive.String))
        object Println : Builtin(Type.Void, listOf(Type.Primitive.String))
        object PrintInt : Builtin(Type.Void, listOf(Type.Primitive.Int))
        object PrintlnInt : Builtin(Type.Void, listOf(Type.Primitive.Int))
        object GetString : Builtin(Type.Primitive.String, listOf())
        object GetInt : Builtin(Type.Primitive.Int, listOf())
        object ToString : Builtin(Type.Primitive.String, listOf(Type.Primitive.Int))
        object StringLength : Builtin(Type.Primitive.Int, listOf())
        object StringSubstring : Builtin(Type.Primitive.String, listOf(Type.Primitive.Int, Type.Primitive.Int))
        object StringParseInt : Builtin(Type.Primitive.Int, listOf())
        object StringOrd : Builtin(Type.Primitive.Int, listOf(Type.Primitive.Int))
        object ArraySize : Builtin(Type.Primitive.Int, listOf())
        class DefaultConstructor(type: Type) : Builtin(type, listOf())
    }
}


