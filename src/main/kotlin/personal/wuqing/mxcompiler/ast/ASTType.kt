package personal.wuqing.mxcompiler.ast

/*
import personal.wuqing.mxcompiler.frontend.ArrayType
import personal.wuqing.mxcompiler.frontend.BoolType
import personal.wuqing.mxcompiler.frontend.FunctionDefinition
import personal.wuqing.mxcompiler.frontend.IntType
import personal.wuqing.mxcompiler.frontend.StringType
import personal.wuqing.mxcompiler.frontend.Type
import personal.wuqing.mxcompiler.frontend.UnknownType
import personal.wuqing.mxcompiler.frontend.VoidType
import personal.wuqing.mxcompiler.utils.Location

fun ClassDeclarationNode.type(): Lazy<Type> = lazy { TODO("based on scope") }

fun NewObjectNode.type() = lazy {
    if (baseType.type is UnknownType) UnknownType
    else TODO("type of new object")
}

fun NewArrayNode.type() = lazy {
    if (baseType.type is UnknownType) UnknownType
    else ArrayType(baseType.type)
}

fun MemberAccessNode.type() = lazy {
    if (parent.type is UnknownType) UnknownType
    else parent.type.variables[child]?.type ?: UnknownType.also {
        LogPrinter.println(astErrorInfo(location, "unknown member $child of $parent"))
    }
}

fun resolveFunction(definition: FunctionDefinition, location: Location): Type = TODO("based on scope")

fun MemberFunctionCallNode.type() = lazy {
    if (parameters.map { it.type }.contains(UnknownType)) UnknownType
    else resolveFunction(FunctionDefinition(base.type, name, parameters.map { it.type }), location)
}

fun FunctionCallNode.type() = lazy {
    if (parameters.map { it.type }.contains(UnknownType)) UnknownType
    else resolveFunction(FunctionDefinition(null, name, parameters.map { it.type }), location)
}

fun IndexAccessNode.type() = lazy {
    when (val p = parent.type) {
        is UnknownType -> UnknownType
        !is ArrayType -> UnknownType.also {
            LogPrinter.println(astErrorInfo(location, "$p cannot be index-accessed"))
        }
        else -> when (val c = child.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                LogPrinter.println(astErrorInfo(location, "type $c cannot be used as index"))
            }
            else -> p.base
        }
    }
}

fun SuffixUnaryNode.type() = lazy {
    when (val o = operand.type) {
        is UnknownType -> UnknownType
        !is IntType -> UnknownType.also {
            LogPrinter.println(astErrorInfo(location, "suffix unary operator $operator cannot be performed on $o"))
        }
        else -> {
            if (operand.lvalue) IntType
            else UnknownType.also {
                LogPrinter.println(astErrorInfo(location, "$operand cannot be assigned"))
            }
        }
    }
}

fun PrefixUnaryNode.type() = lazy {
    when (operator) {
        PrefixUnaryNode.PrefixOperator.INC, PrefixUnaryNode.PrefixOperator.DEC -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                LogPrinter.println(
                    astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o")
                )
            }
            else -> IntType
        }
        PrefixUnaryNode.PrefixOperator.LOGIC_NEGATION -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is BoolType -> UnknownType.also {
                LogPrinter.println(astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o"))
            }
            else -> BoolType
        }
        PrefixUnaryNode.PrefixOperator.ARITHMETIC_NEGATION -> when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                LogPrinter.println(astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o"))
            }
            else -> IntType
        }
        PrefixUnaryNode.PrefixOperator.POSITIVE, PrefixUnaryNode.PrefixOperator.NEGATIVE -> when (val o =
            operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> UnknownType.also {
                LogPrinter.println(astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o"))
            }
            else -> IntType
        }
    }
}

fun BinaryNode.type() = lazy {
    when (operator) {
        BinaryNode.BinaryOperator.PLUS, BinaryNode.BinaryOperator.MINUS,
        BinaryNode.BinaryOperator.TIMES, BinaryNode.BinaryOperator.DIVIDE, BinaryNode.BinaryOperator.REM,
        BinaryNode.BinaryOperator.ARITHMETIC_AND, BinaryNode.BinaryOperator.ARITHMETIC_OR, BinaryNode.BinaryOperator.ARITHMETIC_XOR,
        BinaryNode.BinaryOperator.SHIFT_LEFT, BinaryNode.BinaryOperator.LOGIC_SHIFT_RIGHT, BinaryNode.BinaryOperator.ARITHMETIC_SHIFT_RIGHT -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> IntType
                else -> UnknownType.also {
                    LogPrinter.println(
                        astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r")
                    )
                }
            }
        }
        BinaryNode.BinaryOperator.LOGIC_AND, BinaryNode.BinaryOperator.LOGIC_OR -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is BoolType && r is BoolType -> BoolType
                else -> UnknownType.also {
                    LogPrinter.println(
                        astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r")
                    )
                }
            }
        }
        BinaryNode.BinaryOperator.LESS, BinaryNode.BinaryOperator.LESS_EQUAL, BinaryNode.BinaryOperator.GREATER, BinaryNode.BinaryOperator.GREATER_EQUAL -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> BoolType
                else -> UnknownType.also {
                    LogPrinter.println(
                        astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r")
                    )
                }
            }
        }
        BinaryNode.BinaryOperator.EQUAL, BinaryNode.BinaryOperator.NOT_EQUAL -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType -> IntType
                l is BoolType && r is BoolType -> BoolType
                else -> UnknownType.also {
                    LogPrinter.println(
                        astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r")
                    )
                }
            }
        }
        BinaryNode.BinaryOperator.ASSIGN -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l == r ->
                    if (lhs.lvalue) lhs.type
                    else UnknownType.also {
                        LogPrinter.println(astErrorInfo(location, "$lhs cannot be assigned"))
                    }
                else -> UnknownType.also {
                    LogPrinter.println(
                        astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r")
                    )
                }
            }
        }
        BinaryNode.BinaryOperator.PLUS_ASSIGN, BinaryNode.BinaryOperator.MINUS_ASSIGN,
        BinaryNode.BinaryOperator.TIMES_ASSIGN, BinaryNode.BinaryOperator.DIVIDE_ASSIGN, BinaryNode.BinaryOperator.REM_ASSIGN,
        BinaryNode.BinaryOperator.AND_ASSIGN, BinaryNode.BinaryOperator.OR_ASSIGN, BinaryNode.BinaryOperator.XOR_ASSIGN,
        BinaryNode.BinaryOperator.SHIFT_LEFT_ASSIGN,
        BinaryNode.BinaryOperator.ARITHMETIC_SHIFT_RIGHT_ASSIGN, BinaryNode.BinaryOperator.LOGIC_SHIFT_RIGHT_ASSIGN -> {
            val (l, r) = Pair(lhs.type, rhs.type)
            when {
                l is UnknownType || r is UnknownType -> UnknownType
                l is IntType && r is IntType ->
                    if (lhs.lvalue) IntType
                    else UnknownType.also {
                        LogPrinter.println(astErrorInfo(location, "$lhs cannot be assigned"))
                    }
                else -> UnknownType.also {
                    LogPrinter.println(
                        astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r")
                    )
                }
            }
        }
    }
}

fun TernaryNode.type() = lazy {
    val (t, f) = listOf(thenExpression.type, elseExpression.type)
    when (condition.type) {
        is UnknownType -> UnknownType
        is BoolType -> when {
            t is UnknownType || f is UnknownType -> UnknownType
            t == f -> t
            else -> UnknownType.also {
                LogPrinter.println("types of two alternatives do not match")
            }
        }
        else -> UnknownType.also {
            LogPrinter.println("type of condition must be $BoolType")
        }
    }
}

fun IdentifierExpressionNode.type() = lazy<Type> { TODO("type of identifier") }

fun ThisExpressionNode.type() = lazy<Type> { TODO("type of this") }

fun solveClass(name: String, location: Location): Type = when (name) {
    "int" -> IntType
    "bool" -> BoolType
    "string" -> StringType
    "void" -> VoidType
    else -> TODO("based on scope")
}

fun SimpleTypeNode.type() = lazy { solveClass(name, location) }

fun ArrayTypeNode.type() = lazy {
    var cur = solveClass(name, location)
    if (cur !is UnknownType) repeat(dimension) { cur = ArrayType(cur) }
    cur
}
*/
