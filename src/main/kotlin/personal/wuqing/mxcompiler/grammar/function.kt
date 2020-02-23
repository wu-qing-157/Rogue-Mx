package personal.wuqing.mxcompiler.grammar

import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder
import java.io.Serializable

open class Function(val result: Type, val parameter: List<Type>, val body: ASTNode.Statement.Block?) : Serializable {
    fun match(location: Location, call: List<Type>) =
        if (UnknownType in call || UnknownType in parameter) UnknownType
        else if (call.size != parameter.size || (call zip parameter).any { (c, p) -> c != NullType && c != p })
            UnknownType.also {
                SemanticErrorRecorder.error(
                    location, "cannot call function \"$this\" with \"(${call.joinToString()})\""
                )
            }
        else result
}

sealed class BuiltinFunction(result: Type, parameter: List<Type>) : Function(result, parameter, null) {
    object Print : BuiltinFunction(VoidType, listOf(StringType))
    object Println : BuiltinFunction(VoidType, listOf(StringType))
    object PrintInt : BuiltinFunction(VoidType, listOf(IntType))
    object PrintlnInt : BuiltinFunction(VoidType, listOf(IntType))
    object GetString : BuiltinFunction(StringType, listOf())
    object GetInt : BuiltinFunction(IntType, listOf())
    object ToString : BuiltinFunction(StringType, listOf(IntType))
    object StringLength : BuiltinFunction(IntType, listOf())
    object StringSubstring : BuiltinFunction(StringType, listOf(IntType, IntType))
    object StringParseInt : BuiltinFunction(IntType, listOf())
    object StringOrd : BuiltinFunction(IntType, listOf(IntType))
    object ArraySize : BuiltinFunction(IntType, listOf())

    class DefaultConstructor(type: Type) : BuiltinFunction(type, listOf())

    companion object {

    }
}
