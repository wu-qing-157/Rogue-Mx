package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.grammar.BinaryOperator
import personal.wuqing.mxcompiler.grammar.PrefixOperator
import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.semantic.ClassTable
import personal.wuqing.mxcompiler.semantic.FunctionTable
import personal.wuqing.mxcompiler.semantic.SymbolTable
import personal.wuqing.mxcompiler.semantic.SymbolTableException
import personal.wuqing.mxcompiler.semantic.VariableTable
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder

internal fun ASTNode.Expression.NewObject.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (baseType.type) {
        is Type.Unknown -> Type.Unknown
        is Type.Void -> Type.Unknown.also {
            ASTErrorRecorder.error(location, "cannot construct \"void\" type")
        }
        else -> baseType.type.functions["<constructor>"]?.match(location, parameters.map { it.type })
            ?: Type.Unknown.also {
                ASTErrorRecorder.error(location, "cannot find constructor of \"${baseType.type}\"")
            }
    }
}

internal fun ASTNode.Expression.MemberFunction.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (base.type == Type.Unknown) Type.Unknown
    else base.type.functions[name]?.match(location, parameters.map { it.type })
        ?: Type.Unknown.also {
            ASTErrorRecorder.error(location, "cannot find function \"${base.type}.$name\"")
        }
}

internal fun ASTNode.Expression.Function.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (SymbolTable.thisType.takeIf { it != Type.Unknown } != null
        && SymbolTable.thisType!!.functions[name]?.match(location, parameters.map { it.type })
            .takeIf { it != Type.Unknown } != null
    )
        SymbolTable.thisType!!.functions[name]?.match(location, parameters.map { it.type })!!
    else
        try {
            FunctionTable[name].match(location, parameters.map { it.type })
        } catch (e: SymbolTableException) {
            ASTErrorRecorder.error(location, e.message!!)
            Type.Unknown
        }
}

internal fun ASTNode.Expression.NewArray.type() = lazy(LazyThreadSafetyMode.NONE) {
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

internal fun ASTNode.Expression.MemberAccess.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parent.type is Type.Unknown) Type.Unknown
    else parent.type.variables[child]?.type ?: Type.Unknown.also {
        ASTErrorRecorder.error(location, "unknown member $child of ${parent.type}")
    }
}

internal fun ASTNode.Expression.Index.type() = lazy(LazyThreadSafetyMode.NONE) {
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

internal fun ASTNode.Expression.Suffix.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (val o = operand.type) {
        is Type.Unknown -> Type.Unknown
        !is Type.Primitive.Int -> Type.Unknown.also {
            ASTErrorRecorder.error(location, "suffix unary operator $operator cannot be performed on $o")
        }
        else -> {
            if (operand.lvalue) Type.Primitive.Int
            else Type.Unknown.also {
                ASTErrorRecorder.error(location, "$operand cannot be assigned")
            }
        }
    }
}

internal fun ASTNode.Expression.Prefix.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (operator) {
        PrefixOperator.INC, PrefixOperator.DEC -> when (val o = operand.type) {
            is Type.Unknown -> Type.Unknown
            !is Type.Primitive.Int -> Type.Unknown.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> Type.Primitive.Int
        }
        PrefixOperator.L_NEG -> when (val o = operand.type) {
            is Type.Unknown -> Type.Unknown
            !is Type.Primitive.Bool -> Type.Unknown.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> Type.Primitive.Bool
        }
        PrefixOperator.INV -> when (val o = operand.type) {
            is Type.Unknown -> Type.Unknown
            !is Type.Primitive.Int -> Type.Unknown.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> Type.Primitive.Int
        }
        PrefixOperator.POS, PrefixOperator.NEG -> when (val o = operand.type) {
            is Type.Unknown -> Type.Unknown
            !is Type.Primitive.Int -> Type.Unknown.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> Type.Primitive.Int
        }
    }
}

internal fun ASTNode.Expression.Binary.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (operator) {
        BinaryOperator.PLUS -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l is Type.Primitive.Int && r is Type.Primitive.Int -> Type.Primitive.Int
                l is Type.Primitive.String && r is Type.Primitive.String -> Type.Primitive.String
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.MINUS, BinaryOperator.TIMES, BinaryOperator.DIV, BinaryOperator.REM,
        BinaryOperator.A_AND, BinaryOperator.A_OR, BinaryOperator.A_XOR,
        BinaryOperator.SHL, BinaryOperator.U_SHR, BinaryOperator.SHR -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l is Type.Primitive.Int && r is Type.Primitive.Int -> Type.Primitive.Int
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.L_AND, BinaryOperator.L_OR -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l is Type.Primitive.Bool && r is Type.Primitive.Bool -> Type.Primitive.Bool
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.LESS, BinaryOperator.LEQ, BinaryOperator.GREATER, BinaryOperator.GEQ -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l is Type.Primitive.Int && r is Type.Primitive.Int -> Type.Primitive.Bool
                l is Type.Primitive.String && r is Type.Primitive.String -> Type.Primitive.Bool
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.EQUAL, BinaryOperator.UNEQUAL -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l == r || l == Type.Null || r == Type.Null -> Type.Primitive.Bool
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.ASSIGN -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l !is Type.Primitive && r is Type.Null ->
                    if (lhs.lvalue) lhs.type
                    else Type.Unknown.also {
                        ASTErrorRecorder.error(location, "$lhs cannot be assigned")
                    }
                l == r ->
                    if (lhs.lvalue) lhs.type
                    else Type.Unknown.also {
                        ASTErrorRecorder.error(location, "$lhs cannot be assigned")
                    }
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.PLUS_I, BinaryOperator.MINUS_I,
        BinaryOperator.TIMES_I, BinaryOperator.DIV_I, BinaryOperator.REM_I,
        BinaryOperator.AND_I, BinaryOperator.OR_I, BinaryOperator.XOR_I,
        BinaryOperator.SHL_I,
        BinaryOperator.SHR_I, BinaryOperator.U_SHR_I -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is Type.Unknown || r is Type.Unknown -> Type.Unknown
                l is Type.Primitive.Int && r is Type.Primitive.Int ->
                    if (lhs.lvalue) Type.Primitive.Int
                    else Type.Unknown.also {
                        ASTErrorRecorder.error(location, "$lhs cannot be assigned")
                    }
                else -> Type.Unknown.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
    }
}

internal fun ASTNode.Expression.Ternary.type() = lazy(LazyThreadSafetyMode.NONE) {
    val (t, f) = listOf(then.type, else_.type)
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

internal fun ASTNode.Expression.Identifier.type() = lazy(LazyThreadSafetyMode.NONE) {
    try {
        VariableTable[name].type
    } catch (e: SymbolTableException) {
        SymbolTable.thisType?.variables?.get(name)?.type ?: Type.Unknown.also {
            ASTErrorRecorder.error(location, e.message!!)
        }
    }
}

internal fun ASTNode.Expression.This.type() = lazy(LazyThreadSafetyMode.NONE) {
    SymbolTable.thisType ?: Type.Unknown.also { ASTErrorRecorder.error(location, "cannot resolve \"this\"") }
}

internal fun solveClass(name: String, location: Location): Type = when (name) {
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

internal fun ASTNode.Type.Simple.type() = lazy(LazyThreadSafetyMode.NONE) {
    solveClass(name, location)
}

internal fun ASTNode.Type.Array.type() = lazy(LazyThreadSafetyMode.NONE) {
    Type.Array.get(solveClass(name, location), dimension, location)
}
