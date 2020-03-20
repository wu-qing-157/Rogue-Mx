package personal.wuqing.rogue.ast

import personal.wuqing.rogue.grammar.MxType
import personal.wuqing.rogue.semantic.table.ClassTable
import personal.wuqing.rogue.semantic.table.FunctionTable
import personal.wuqing.rogue.semantic.table.SymbolTable
import personal.wuqing.rogue.semantic.table.SymbolTableException
import personal.wuqing.rogue.semantic.table.VariableTable
import personal.wuqing.rogue.utils.ASTErrorRecorder
import personal.wuqing.rogue.utils.Location
import personal.wuqing.rogue.utils.SemanticErrorRecorder

fun ASTNode.Expression.NewObject.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (baseType.type) {
        is MxType.Unknown -> MxType.Unknown
        is MxType.Void -> MxType.Unknown.also {
            ASTErrorRecorder.error(location, "cannot construct \"void\" type")
        }
        else -> baseType.type.functions["__constructor__"]?.match(location, parameters.map { it.type })
            ?: MxType.Unknown.also {
                ASTErrorRecorder.error(location, "cannot find constructor of \"${baseType.type}\"")
            }
    }
}

fun ASTNode.Expression.MemberFunction.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    base.type.functions[name]
}

fun ASTNode.Expression.MemberFunction.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (base.type == MxType.Unknown) MxType.Unknown
    else resolved?.match(location, parameters.map { it.type })
        ?: MxType.Unknown.also {
            ASTErrorRecorder.error(location, "cannot find function \"${base.type}.$name\"")
        }
}

fun ASTNode.Expression.Function.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    if (SymbolTable.thisType != null
        && SymbolTable.thisType!!.functions[name]?.match(location, parameters.map { it.type })
            .takeIf { it != MxType.Unknown } != null
    ) SymbolTable.thisType!!.functions[name]
    else try {
        FunctionTable[name]
    } catch (e: SymbolTableException) {
        null
    }
}

fun ASTNode.Expression.Function.type() = lazy(LazyThreadSafetyMode.NONE) {
    resolved?.match(location, parameters.map { it.type }) ?: MxType.Unknown.also {
        ASTErrorRecorder.error(location, "cannot resolve $name as a function")
    }
}

fun ASTNode.Expression.NewArray.type() = lazy(LazyThreadSafetyMode.NONE) {
    length.map { it.type }.run {
        when {
            any { it == MxType.Unknown } -> MxType.Unknown
            any { it != MxType.Primitive.Int } -> MxType.Unknown.also {
                SemanticErrorRecorder.error(location, "length or array must be \"int\"")
            }
            else -> MxType.Array.get(baseType.type, dimension, location)
        }
    }
}

fun ASTNode.Expression.MemberAccess.resolve() = lazy(LazyThreadSafetyMode.NONE) {
    (parent.type as? MxType.Class)?.variables?.get(child)
}

fun ASTNode.Expression.MemberAccess.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parent.type is MxType.Unknown) MxType.Unknown
    else resolved?.type ?: MxType.Unknown.also {
        ASTErrorRecorder.error(location, "unknown member $child of ${parent.type}")
    }
}

fun ASTNode.Expression.Index.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (val p = parent.type) {
        is MxType.Unknown -> MxType.Unknown
        !is MxType.Array -> MxType.Unknown.also {
            ASTErrorRecorder.error(location, "$p cannot be index-accessed")
        }
        else -> when (val c = child.type) {
            is MxType.Unknown -> MxType.Unknown
            !is MxType.Primitive.Int -> MxType.Unknown.also {
                ASTErrorRecorder.error(location, "type $c cannot be used as index")
            }
            else -> p.base
        }
    }
}

fun ASTNode.Expression.Suffix.type() = lazy(LazyThreadSafetyMode.NONE) {
    operator.accept(operand.type, operand.lvalue) ?: MxType.Unknown.also {
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
    operator.accept(operand.type, operator.lvalue) ?: MxType.Unknown.also {
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
    operator.accept(lhs.type to lhs.lvalue, rhs.type to rhs.lvalue) ?: MxType.Unknown.also {
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
        is MxType.Unknown -> MxType.Unknown
        is MxType.Primitive.Bool -> when {
            t is MxType.Unknown || f is MxType.Unknown -> MxType.Unknown
            t == f -> t
            else -> MxType.Unknown.also {
                ASTErrorRecorder.error(location, "types of two alternatives do not match")
            }
        }
        else -> MxType.Unknown.also {
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
    resolved?.second?.type ?: MxType.Unknown.also {
        SemanticErrorRecorder.error(location, "cannot resolve \"$name\" as a variable")
    }
}

fun ASTNode.Expression.This.type() = lazy(LazyThreadSafetyMode.NONE) {
    SymbolTable.thisType ?: MxType.Unknown.also { ASTErrorRecorder.error(location, "cannot resolve \"this\"") }
}

fun solveClass(name: String, location: Location): MxType = when (name) {
    "int" -> MxType.Primitive.Int
    "bool" -> MxType.Primitive.Bool
    "string" -> MxType.Primitive.String
    "void" -> MxType.Void
    else -> try {
        ClassTable[name]
    } catch (e: SymbolTableException) {
        ASTErrorRecorder.error(location, e.message!!)
        MxType.Unknown
    }
}

fun ASTNode.Type.Simple.type() = lazy(LazyThreadSafetyMode.NONE) {
    solveClass(name, location)
}

fun ASTNode.Type.Array.type() = lazy(LazyThreadSafetyMode.NONE) {
    MxType.Array.get(solveClass(name, location), dimension, location)
}
