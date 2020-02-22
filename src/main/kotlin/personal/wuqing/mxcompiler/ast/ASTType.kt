package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.frontend.ArrayType
import personal.wuqing.mxcompiler.frontend.BinaryOperator
import personal.wuqing.mxcompiler.frontend.BoolType
import personal.wuqing.mxcompiler.frontend.ClassTable
import personal.wuqing.mxcompiler.frontend.FunctionDefinition
import personal.wuqing.mxcompiler.frontend.FunctionTable
import personal.wuqing.mxcompiler.frontend.IntType
import personal.wuqing.mxcompiler.frontend.PrefixOperator
import personal.wuqing.mxcompiler.frontend.StringType
import personal.wuqing.mxcompiler.frontend.SymbolTable
import personal.wuqing.mxcompiler.frontend.Type
import personal.wuqing.mxcompiler.frontend.UnknownType
import personal.wuqing.mxcompiler.frontend.VariableTable
import personal.wuqing.mxcompiler.frontend.VoidType
import personal.wuqing.mxcompiler.utils.Location

internal fun resolveFunction(definition: FunctionDefinition, location: Location) =
    try {
        FunctionTable[definition].returnType
    } catch (e: SymbolTable.NotFoundException) {
        ASTErrorRecorder.error(location, "cannot resolve $definition as a internal function")
        UnknownType
    }

internal fun ASTNode.Expression.NewObject.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (baseType.type is UnknownType) UnknownType
    else resolveFunction(FunctionDefinition(baseType.type, "<constructor>", parameters.map { it.type }), location)
}

internal fun ASTNode.Expression.MemberFunction.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parameters.map { it.type }.contains(UnknownType)) UnknownType
    else resolveFunction(FunctionDefinition(base.type, name, parameters.map { it.type }), location)
}

internal fun ASTNode.Expression.Function.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parameters.map { it.type }.contains(UnknownType)) UnknownType
    else resolveFunction(FunctionDefinition(null, name, parameters.map { it.type }), location)
}

internal fun ASTNode.Expression.NewArray.type() = lazy(LazyThreadSafetyMode.NONE) {
    var cur = baseType.type
    if (cur !is UnknownType) repeat(dimension) { cur = ArrayType(cur) }
    cur
}

internal fun ASTNode.Expression.MemberAccess.type() = lazy(LazyThreadSafetyMode.NONE) {
    if (parent.type is UnknownType) UnknownType
    else parent.type.variables[child]?.type ?: UnknownType.also {
        ASTErrorRecorder.error(location, "unknown member $child of $parent")
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
        BinaryOperator.PLUS, BinaryOperator.MINUS,
        BinaryOperator.TIMES, BinaryOperator.DIV, BinaryOperator.REM,
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
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.EQUAL, BinaryOperator.UNEQUAL -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> BoolType
                l is BoolType && r is BoolType -> BoolType
                else -> UnknownType.also {
                    ASTErrorRecorder.error(location, "binary operator $operator cannot be performed on $l and $r")
                }
            }
        }
        BinaryOperator.ASSIGN -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
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
    } catch (e: SymbolTable.NotFoundException) {
        ASTErrorRecorder.error(location, "cannot resolve \"$name\" as a variable")
        UnknownType
    }
}

internal fun ASTNode.Expression.This.type() = lazy(LazyThreadSafetyMode.NONE) {
    SymbolTable.thisType().also {
        if (it == UnknownType) ASTErrorRecorder.error(location, "cannot resolve \"this\"")
    }
}

internal fun solveClass(name: String, location: Location): Type = when (name) {
    "int" -> IntType
    "bool" -> BoolType
    "string" -> StringType
    "void" -> VoidType
    else -> try {
        ClassTable[name]
    } catch (e: SymbolTable.NotFoundException) {
        ASTErrorRecorder.error(location, "cannot resolve \"$name\" as a class")
        UnknownType
    }
}

internal fun ASTNode.Type.Simple.type() = lazy(LazyThreadSafetyMode.NONE) {
    solveClass(name, location)
}

internal fun ASTNode.Type.Array.type() = lazy(LazyThreadSafetyMode.NONE) {
    var cur = solveClass(name, location)
    if (cur !is UnknownType) repeat(dimension) { cur = ArrayType(cur) }
    cur
}
