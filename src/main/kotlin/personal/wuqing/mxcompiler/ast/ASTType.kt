package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.semantic.ClassTable
import personal.wuqing.mxcompiler.semantic.FunctionTable
import personal.wuqing.mxcompiler.semantic.SymbolTable
import personal.wuqing.mxcompiler.semantic.SymbolTableException
import personal.wuqing.mxcompiler.semantic.VariableTable
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder

fun ASTNode.Expression.NewObject.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (baseType.type) {
        is Type.Unknown -> Type.Unknown
        is Type.Void -> Type.Unknown.also {
            ASTErrorRecorder.error(location, "cannot construct \"void\" type")
        }
        else -> baseType.type.functions["__constructor__"]?.match(location, parameters.map { it.type })
            ?: Type.Unknown.also {
                ASTErrorRecorder.error(location, "cannot find constructor of \"${baseType.type}\"")
            }
    }
}

fun ASTNode.Expression.MemberFunction.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    base.type.functions[name]
}

fun ASTNode.Expression.MemberFunction.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (base.type == Type.Unknown) Type.Unknown
    else resolved?.match(location, parameters.map { it.type })
        ?: Type.Unknown.also {
            ASTErrorRecorder.error(location, "cannot find function \"${base.type}.$name\"")
        }
}

fun ASTNode.Expression.Function.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    if (SymbolTable.thisType != null
        && SymbolTable.thisType!!.functions[name]?.match(location, parameters.map { it.type })
            .takeIf { it != Type.Unknown } != null
    ) SymbolTable.thisType!!.functions[name]
    else try {
        FunctionTable[name]
    } catch (e: SymbolTableException) {
        null
    }
}

fun ASTNode.Expression.Function.type() = lazy(LazyThreadSafetyMode.NONE) {
    resolved?.match(location, parameters.map { it.type }) ?: Type.Unknown.also {
        ASTErrorRecorder.error(location, "cannot resolve $name as a function")
    }
}

fun ASTNode.Expression.NewArray.type() = lazy(LazyThreadSafetyMode.NONE) {
    length.map { it.type }.run {
        when {
            any { it == Type.Unknown } -> Type.Unknown
            any { it != Type.Primitive.Int } -> Type.Unknown.also {
                SemanticErrorRecorder.error(location, "length or array must be \"int\"")
            }
            else -> Type.Array.get(baseType.type, dimension, location)
        }
    }
}

fun ASTNode.Expression.MemberAccess.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    (parent.type as? Type.Class)?.variables?.get(child)
}

fun ASTNode.Expression.MemberAccess.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parent.type is Type.Unknown) Type.Unknown
    else resolved?.type ?: Type.Unknown.also {
        ASTErrorRecorder.error(location, "unknown member $child of ${parent.type}")
    }
}

fun ASTNode.Expression.Index.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (val p = parent.type) {
        is Type.Unknown -> Type.Unknown
        !is Type.Array -> Type.Unknown.also {
            ASTErrorRecorder.error(location, "$p cannot be index-accessed")
        }
        else -> when (val c = child.type) {
            is Type.Unknown -> Type.Unknown
            !is Type.Primitive.Int -> Type.Unknown.also {
                ASTErrorRecorder.error(location, "type $c cannot be used as index")
            }
            else -> p.base
        }
    }
}

fun ASTNode.Expression.Suffix.type() = lazy(LazyThreadSafetyMode.NONE) {
    operator.accept(operand.type, operand.lvalue) ?: Type.Unknown.also {
        ASTErrorRecorder.error(
            location,
            if (operator.accept(operand.type, true) == null)
                "suffix operator $operator cannot be performed on ${operand.type}"
            else
                "suffix operator $operator contains an invalid assignment"
        )
    }
}

fun ASTNode.Expression.Prefix.type() = lazy(LazyThreadSafetyMode.NONE) {
    operator.accept(operand.type, operator.lvalue) ?: Type.Unknown.also {
        ASTErrorRecorder.error(
            location,
            if (operator.accept(operand.type, true) == null)
                "prefix operator $operator cannot be performed on ${operand.type}"
            else
                "prefix operator $operator contains an invalid assignment"
        )
    }
}

fun ASTNode.Expression.Binary.type() = lazy(LazyThreadSafetyMode.NONE) {
    operator.accept(lhs.type to lhs.lvalue, rhs.type to rhs.lvalue) ?: Type.Unknown.also {
        ASTErrorRecorder.error(
            location,
            if (operator.accept(lhs.type to true, rhs.type to true) == null)
                "binary operator $operator cannot be performed on ${lhs.type} and ${rhs.type}"
            else
                "binary operator $operator contains an invalid assignment"
        )
    }
}

fun ASTNode.Expression.Ternary.type() = lazy(LazyThreadSafetyMode.NONE) {
    val (t, f) = listOf(then.type, els.type)
    when (condition.type) {
        is Type.Unknown -> Type.Unknown
        is Type.Primitive.Bool -> when {
            t is Type.Unknown || f is Type.Unknown -> Type.Unknown
            t == f -> t
            else -> Type.Unknown.also {
                ASTErrorRecorder.error(location, "types of two alternatives do not match")
            }
        }
        else -> Type.Unknown.also {
            ASTErrorRecorder.error(location, "type of condition must be \"bool\"")
        }
    }
}

enum class ReferenceType { Variable, Member }

fun ASTNode.Expression.Identifier.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    try {
        val table = VariableTable[name]
        val member = SymbolTable.thisType?.variables?.get(name)
        if (table == member) ReferenceType.Member to member
        else ReferenceType.Variable to table
    } catch (e: SymbolTableException) {
        null
    }
}

fun ASTNode.Expression.Identifier.type() = lazy(LazyThreadSafetyMode.NONE) {
    resolved?.second?.type ?: Type.Unknown.also {
        SemanticErrorRecorder.error(location, "cannot resolve \"$name\" as a variable")
    }
}

fun ASTNode.Expression.This.type() = lazy(LazyThreadSafetyMode.NONE) {
    SymbolTable.thisType ?: Type.Unknown.also { ASTErrorRecorder.error(location, "cannot resolve \"this\"") }
}

fun solveClass(name: String, location: Location): Type = when (name) {
    "int" -> Type.Primitive.Int
    "bool" -> Type.Primitive.Bool
    "string" -> Type.Primitive.String
    "void" -> Type.Void
    else -> try {
        ClassTable[name]
    } catch (e: SymbolTableException) {
        ASTErrorRecorder.error(location, e.message!!)
        Type.Unknown
    }
}

fun ASTNode.Type.Simple.type() = lazy(LazyThreadSafetyMode.NONE) {
    solveClass(name, location)
}

fun ASTNode.Type.Array.type() = lazy(LazyThreadSafetyMode.NONE) {
    Type.Array.get(solveClass(name, location), dimension, location)
}
