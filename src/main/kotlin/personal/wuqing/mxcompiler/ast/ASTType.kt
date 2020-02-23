package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.grammar.ArrayType
import personal.wuqing.mxcompiler.grammar.BinaryOperator
import personal.wuqing.mxcompiler.grammar.BoolType
import personal.wuqing.mxcompiler.grammar.IntType
import personal.wuqing.mxcompiler.grammar.NullType
import personal.wuqing.mxcompiler.grammar.PrefixOperator
import personal.wuqing.mxcompiler.grammar.PrimitiveType
import personal.wuqing.mxcompiler.grammar.StringType
import personal.wuqing.mxcompiler.grammar.Type
import personal.wuqing.mxcompiler.grammar.UnknownType
import personal.wuqing.mxcompiler.grammar.VoidType
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
        is UnknownType -> UnknownType
        is VoidType -> UnknownType.also {
            ASTErrorRecorder.error(location, "cannot construct \"void\" type")
        }
        else -> baseType.type.functions["<constructor>"]?.match(location, parameters.map { it.type })
            ?: UnknownType.also {
                ASTErrorRecorder.error(location, "cannot find constructor of \"${baseType.type}\"")
            }
    }
}

internal fun ASTNode.Expression.MemberFunction.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (base.type == UnknownType) UnknownType
    else base.type.functions[name]?.match(location, parameters.map { it.type })
        ?: UnknownType.also {
            ASTErrorRecorder.error(location, "cannot find function \"${base.type}.$name\"")
        }
}

internal fun ASTNode.Expression.Function.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (SymbolTable.thisType.takeIf { it != UnknownType } != null
        && SymbolTable.thisType!!.functions[name]?.match(location, parameters.map { it.type })
            .takeIf { it != UnknownType } != null
    )
        SymbolTable.thisType!!.functions[name]?.match(location, parameters.map { it.type })!!
    else
        try {
            FunctionTable[name].match(location, parameters.map { it.type })
        } catch (e: SymbolTableException) {
            ASTErrorRecorder.error(location, e.toString())
            UnknownType
        }
}

internal fun ASTNode.Expression.NewArray.type() = lazy(LazyThreadSafetyMode.NONE) {
    length.filterNotNull().map { it.type }.run {
        when {
            any { it == UnknownType } -> UnknownType
            any { it != IntType } -> UnknownType.also {
                SemanticErrorRecorder.error(location, "length or array must be \"int\"")
            }
            else -> ArrayType.getArrayType(baseType.type, dimension, location)
        }
    }
}

internal fun ASTNode.Expression.MemberAccess.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parent.type is UnknownType) UnknownType
    else parent.type.variables[child]?.type ?: UnknownType.also {
        ASTErrorRecorder.error(location, "unknown member $child of ${parent.type}")
    }
}

internal fun ASTNode.Expression.Index.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (val p = parent.type) {
        is UnknownType -> UnknownType
        !is ArrayType -> UnknownType.also {
            ASTErrorRecorder.error(location, "$p cannot be index-accessed")
        }
        else -> when (val c = child.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                ASTErrorRecorder.error(location, "type $c cannot be used as index")
            }
            else -> p.base
        }
    }
}

internal fun ASTNode.Expression.Suffix.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (val o = operand.type) {
        is UnknownType -> UnknownType
        !is IntType -> UnknownType.also {
            ASTErrorRecorder.error(location, "suffix unary operator $operator cannot be performed on $o")
        }
        else -> {
            if (operand.lvalue) IntType
            else UnknownType.also {
                ASTErrorRecorder.error(location, "$operand cannot be assigned")
            }
        }
    }
}

internal fun ASTNode.Expression.Prefix.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (operator) {
        PrefixOperator.INC, PrefixOperator.DEC -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> IntType
        }
        PrefixOperator.L_NEG -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is BoolType -> UnknownType.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> BoolType
        }
        PrefixOperator.INV -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> IntType
        }
        PrefixOperator.POS, PrefixOperator.NEG -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                ASTErrorRecorder.error(location, "prefix unary operator $operator cannot be performed on $o")
            }
            else -> IntType
        }
    }
}

internal fun ASTNode.Expression.Binary.type() = lazy(LazyThreadSafetyMode.NONE) {
    when (operator) {
        BinaryOperator.PLUS -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> IntType
                l is StringType && r is StringType -> StringType
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.MINUS, BinaryOperator.TIMES, BinaryOperator.DIV, BinaryOperator.REM,
        BinaryOperator.A_AND, BinaryOperator.A_OR, BinaryOperator.A_XOR,
        BinaryOperator.SHL, BinaryOperator.U_SHR, BinaryOperator.SHR -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> IntType
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.L_AND, BinaryOperator.L_OR -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is BoolType && r is BoolType -> BoolType
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.LESS, BinaryOperator.LEQ, BinaryOperator.GREATER, BinaryOperator.GEQ -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> BoolType
                l is StringType && r is StringType -> BoolType
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.EQUAL, BinaryOperator.UNEQUAL -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l == r || l == NullType || r == NullType -> BoolType
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.ASSIGN -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l !is PrimitiveType && r is NullType ->
                    if (lhs.lvalue) lhs.type
                    else UnknownType.also {
                        ASTErrorRecorder.error(location, "$lhs cannot be assigned")
                    }
                l == r ->
                    if (lhs.lvalue) lhs.type
                    else UnknownType.also {
                        ASTErrorRecorder.error(location, "$lhs cannot be assigned")
                    }
                else -> UnknownType.also {
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
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType ->
                    if (lhs.lvalue) IntType
                    else UnknownType.also {
                        ASTErrorRecorder.error(location, "$lhs cannot be assigned")
                    }
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
    }
}

internal fun ASTNode.Expression.Ternary.type() = lazy(LazyThreadSafetyMode.NONE) {
    val (t, f) = listOf(then.type, else_.type)
    when (condition.type) {
        is UnknownType -> UnknownType
        is BoolType -> when {
            t is UnknownType || f is UnknownType -> UnknownType
            t == f -> t
            else -> UnknownType.also {
                ASTErrorRecorder.error(location, "types of two alternatives do not match")
            }
        }
        else -> UnknownType.also {
            ASTErrorRecorder.error(location, "type of condition must be \"bool\"")
        }
    }
}

internal fun ASTNode.Expression.Identifier.type() = lazy(LazyThreadSafetyMode.NONE) {
    try {
        VariableTable[name].type
    } catch (e: SymbolTableException) {
        SymbolTable.thisType?.variables?.get(name)?.type ?: UnknownType.also {
            ASTErrorRecorder.error(location, e.toString())
        }
    }
}

internal fun ASTNode.Expression.This.type() = lazy(LazyThreadSafetyMode.NONE) {
    SymbolTable.thisType ?: UnknownType.also { ASTErrorRecorder.error(location, "cannot resolve \"this\"") }
}

internal fun solveClass(name: String, location: Location): Type = when (name) {
    "int" -> IntType
    "bool" -> BoolType
    "string" -> StringType
    "void" -> VoidType
    else -> try {
        ClassTable[name]
    } catch (e: SymbolTableException) {
        ASTErrorRecorder.error(location, e.toString())
        UnknownType
    }
}

internal fun ASTNode.Type.Simple.type() = lazy(LazyThreadSafetyMode.NONE) {
    solveClass(name, location)
}

internal fun ASTNode.Type.Array.type() = lazy(LazyThreadSafetyMode.NONE) {
    ArrayType.getArrayType(solveClass(name, location), dimension, location)
}
