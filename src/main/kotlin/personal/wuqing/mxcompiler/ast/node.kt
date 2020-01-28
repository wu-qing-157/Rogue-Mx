package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.astErrorInfo
import personal.wuqing.mxcompiler.type.ArrayType
import personal.wuqing.mxcompiler.type.BoolType
import personal.wuqing.mxcompiler.type.FunctionType
import personal.wuqing.mxcompiler.type.IntType
import personal.wuqing.mxcompiler.type.Type
import personal.wuqing.mxcompiler.type.UnknownType
import personal.wuqing.mxcompiler.type.VoidType
import personal.wuqing.mxcompiler.utils.Location

sealed class ASTNode {
    protected abstract val location: Location
}

class ProgramNode(
    override val location: Location,
    val declarations: List<DeclarationNode>
) : ASTNode()

sealed class DeclarationNode : ASTNode()

class ParameterNode(
    override val location: Location, val name: String,
    val type: TypeNode
) : ASTNode()

class FunctionDeclarationNode(
    override val location: Location, val name: String,
    val returnType: TypeNode, val parameterList: List<ParameterNode>
) : DeclarationNode()

class VariableDeclarationListNode(
    override val location: Location,
    val list: List<VariableDeclarationNode>
) : ASTNode()

class VariableDeclarationNode(
    override val location: Location, val name: String,
    val type: TypeNode, val init: ExpressionNode?
) : DeclarationNode()

class ClassDeclarationNode(
    override val location: Location, val name: String,
    val variables: List<VariableDeclarationNode>
) : DeclarationNode()

sealed class StatementNode : ASTNode()

class BlockNode(
    override val location: Location,
    val statements: List<StatementNode>
) : StatementNode()

class ExpressionStatementNode(
    override val location: Location,
    val expression: ExpressionNode
) : StatementNode()

class VariableDeclarationStatementNode(
    override val location: Location,
    val variables: List<VariableDeclarationNode>
) : StatementNode()

class IfNode(
    override val location: Location,
    val condition: ExpressionNode, val thenStatement: StatementNode, val elseStatement: StatementNode?
) : StatementNode()

class WhileNode(
    override val location: Location,
    val condition: ExpressionNode, val statement: StatementNode
) : StatementNode()

class ForNode(
    override val location: Location,
    val initVariableDeclaration: List<VariableDeclarationNode>, val initExpression: ExpressionNode?,
    val condition: ExpressionNode, val step: ExpressionNode?, val statement: StatementNode
) : StatementNode()

class ContinueNode(
    override val location: Location
) : StatementNode()

class BreakNode(
    override val location: Location
) : StatementNode()

class ReturnNode(
    override val location: Location,
    val expression: ExpressionNode
) : StatementNode()

class BlankStatementNode(
    override val location: Location
) : StatementNode()

sealed class ExpressionNode : ASTNode() {
    abstract val type: Type
    abstract val lvalue: Boolean
}

class MemberAccessNode(
    override val location: Location,
    private val parent: ExpressionNode, private val child: String
) : ExpressionNode() {
    override val lvalue = true
    override val type by lazy {
        if (parent.type is UnknownType) UnknownType
        else parent.type.members[child] ?: {
            println(astErrorInfo(location, "unknown member $child of $parent"))
            UnknownType
        }()
    }
}

class NewOperatorNode(
    override val location: Location,
    private val baseType: TypeNode, private val length: Int?
) : ExpressionNode() {
    override val lvalue = true
    override val type by lazy {
        if (baseType.type is UnknownType) UnknownType
        else ArrayType(baseType.type)
    }
}

class FunctionCallNode(
    override val location: Location,
    private val name: String,
    private val function: ExpressionNode, private val parameters: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = false
    override val type by lazy {
        when (val f = function.type) {
            is UnknownType -> UnknownType
            !is FunctionType -> {
                println(astErrorInfo(location, "$name is not a function"))
                UnknownType
            }
            else -> when {
                parameters.any { it.type == UnknownType } -> UnknownType
                f.parameterList != parameters.map { it.type } -> {
                    println(
                        astErrorInfo(
                            location,
                            "$name has a signature of (${f.parameterList.joinToString()}), " +
                                    "but (${parameters.map { it.type }}) was found"
                        )
                    )
                    UnknownType
                }
                else -> f.returnType
            }
        }
    }
}

class IndexAccessNode(
    override val location: Location,
    private val parent: ExpressionNode, private val child: ExpressionNode
) : ExpressionNode() {
    override val lvalue = true
    override val type by lazy {
        when (val p = parent.type) {
            is UnknownType -> UnknownType
            !is ArrayType -> {
                println(astErrorInfo(location, "$p cannot be index-accessed"))
                UnknownType
            }
            else -> when (val c = child.type) {
                is UnknownType -> UnknownType
                !is IntType -> {
                    println(astErrorInfo(location, "type $c cannot be used as index"))
                    UnknownType
                }
                else -> p.base
            }
        }
    }
}

class SuffixUnaryNode(
    override val location: Location,
    private val operand: ExpressionNode, private val operator: SuffixOperator
) : ExpressionNode() {
    enum class SuffixOperator { INC, DEC }

    override val lvalue = false
    override val type by lazy {
        when (val o = operand.type) {
            is UnknownType -> UnknownType
            !is IntType -> {
                println(astErrorInfo(location, "suffix unary operator $operator cannot be performed on $o"))
                UnknownType
            }
            else ->
                if (operand.lvalue) IntType
                else {
                    println(astErrorInfo(location, "$operand cannot be assigned"))
                    UnknownType
                }
        }
    }
}

class PrefixUnaryNode(
    override val location: Location,
    private val operand: ExpressionNode, private val operator: PrefixOperator
) : ExpressionNode() {
    enum class PrefixOperator(private val displayText: String) {
        INC("++"), DEC("--"),
        LOGIC_NEGATION("!"), ARITHMETIC_NEGATION("~"),
        POSITIVE("+"), NEGATIVE("-");

        override fun toString() = displayText
    }

    override val lvalue = when (operator) {
        PrefixOperator.INC, PrefixOperator.DEC -> true
        PrefixOperator.LOGIC_NEGATION, PrefixOperator.ARITHMETIC_NEGATION,
        PrefixOperator.POSITIVE, PrefixOperator.NEGATIVE -> false
    }
    override val type by lazy {
        when (operator) {
            PrefixOperator.INC, PrefixOperator.DEC -> when (val o = operand.type) {
                is UnknownType -> UnknownType
                !is IntType -> {
                    println(
                        astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o")
                    )
                    UnknownType
                }
                else -> IntType
            }
            PrefixOperator.LOGIC_NEGATION -> when (val o = operand.type) {
                is UnknownType -> UnknownType
                !is BoolType -> {
                    println(astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o"))
                    UnknownType
                }
                else -> BoolType
            }
            PrefixOperator.ARITHMETIC_NEGATION -> when (val o = operand.type) {
                is UnknownType -> UnknownType
                !is IntType -> {
                    println(astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o"))
                    UnknownType
                }
                else -> IntType
            }
            PrefixOperator.POSITIVE, PrefixOperator.NEGATIVE -> when (val o = operand.type) {
                is UnknownType -> UnknownType
                !is IntType -> {
                    println(astErrorInfo(location, "prefix unary operator $operator cannot be performed on $o"))
                    UnknownType
                }
                else -> IntType
            }
        }
    }
}

class BinaryNode(
    override val location: Location,
    private val lhs: ExpressionNode, private val rhs: ExpressionNode, private val operator: BinaryOperator
) : ExpressionNode() {
    enum class BinaryOperator(private val displayText: String) {
        PLUS("+"), MINUS("-"),
        TIMES("*"), DIVIDE("/"), REM("%"),
        LOGIC_AND("&&"), LOGIC_OR("||"),
        ARITHMETIC_AND("&"), ARITHMETIC_OR("|"), ARITHMETIC_XOR("^"),
        SHIFT_LEFT("<<"), LOGIC_SHIFT_RIGHT(">>>"), ARITHMETIC_SHIFT_RIGHT(">>"),
        LESS("<"), LESS_EQUAL("<="),
        GREATER(">"), GREATER_EQUAL(">="),
        EQUAL("=="), NOT_EQUAL("!="),
        ASSIGN("="),
        PLUS_ASSIGN("+="), MINUS_ASSIGN("-="),
        TIMES_ASSIGN("*="), DIVIDE_ASSIGN("/="), REM_ASSIGN("%="),
        AND_ASSIGN("&="), OR_ASSIGN("|="), XOR_ASSIGN("^="),
        SHIFT_LEFT_ASSIGN("<<="),
        LOGIC_SHIFT_RIGHT_ASSIGN(">>>="), ARITHMETIC_SHIFT_RIGHT_ASSIGN(">>=");

        override fun toString() = displayText
    }

    override val lvalue = false

    override val type by lazy {
        when (operator) {
            BinaryOperator.PLUS, BinaryOperator.MINUS,
            BinaryOperator.TIMES, BinaryOperator.DIVIDE, BinaryOperator.REM,
            BinaryOperator.ARITHMETIC_AND, BinaryOperator.ARITHMETIC_OR, BinaryOperator.ARITHMETIC_XOR,
            BinaryOperator.SHIFT_LEFT, BinaryOperator.LOGIC_SHIFT_RIGHT, BinaryOperator.ARITHMETIC_SHIFT_RIGHT -> {
                val (l, r) = Pair(lhs.type, rhs.type)
                when {
                    l is UnknownType || r is UnknownType -> UnknownType
                    l is IntType && r is IntType -> IntType
                    else -> {
                        println(astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r"))
                        UnknownType
                    }
                }
            }
            BinaryOperator.LOGIC_AND, BinaryOperator.LOGIC_OR -> {
                val (l, r) = Pair(lhs.type, rhs.type)
                when {
                    l is UnknownType || r is UnknownType -> UnknownType
                    l is BoolType && r is BoolType -> BoolType
                    else -> {
                        println(astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r"))
                        UnknownType
                    }
                }
            }
            BinaryOperator.LESS, BinaryOperator.LESS_EQUAL, BinaryOperator.GREATER, BinaryOperator.GREATER_EQUAL -> {
                val (l, r) = Pair(lhs.type, rhs.type)
                when {
                    l is UnknownType || r is UnknownType -> UnknownType
                    l is IntType && r is IntType -> BoolType
                    else -> {
                        println(astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r"))
                        UnknownType
                    }
                }
            }
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL -> {
                val (l, r) = Pair(lhs.type, rhs.type)
                when {
                    l is UnknownType || r is UnknownType -> UnknownType
                    l is IntType && r is IntType -> IntType
                    l is BoolType && r is BoolType -> BoolType
                    else -> {
                        println(astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r"))
                        UnknownType
                    }
                }
            }
            BinaryOperator.ASSIGN -> {
                val (l, r) = Pair(lhs.type, rhs.type)
                when {
                    l is UnknownType || r is UnknownType -> UnknownType
                    l == r ->
                        if (lhs.lvalue) VoidType
                        else {
                            println(astErrorInfo(location, "$lhs cannot be assigned"))
                            UnknownType
                        }
                    else -> {
                        println(astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r"))
                        UnknownType
                    }
                }
            }
            BinaryOperator.PLUS_ASSIGN, BinaryOperator.MINUS_ASSIGN,
            BinaryOperator.TIMES_ASSIGN, BinaryOperator.DIVIDE_ASSIGN, BinaryOperator.REM_ASSIGN,
            BinaryOperator.AND_ASSIGN, BinaryOperator.OR_ASSIGN, BinaryOperator.XOR_ASSIGN,
            BinaryOperator.SHIFT_LEFT_ASSIGN,
            BinaryOperator.ARITHMETIC_SHIFT_RIGHT_ASSIGN, BinaryOperator.LOGIC_SHIFT_RIGHT_ASSIGN -> {
                val (l, r) = Pair(lhs.type, rhs.type)
                when {
                    l is UnknownType || r is UnknownType -> UnknownType
                    l is IntType && r is IntType ->
                        if (lhs.lvalue) VoidType
                        else {
                            println(astErrorInfo(location, "$lhs cannot be assigned"))
                            UnknownType
                        }
                    else -> {
                        println(astErrorInfo(location, "binary operator $operator cannot be performed on $l and $r"))
                        UnknownType
                    }
                }
            }
        }
    }
}

class TernaryNode(
    override val location: Location,
    private val condition: ExpressionNode,
    private val trueExpression: ExpressionNode, private val falseExpression: ExpressionNode
) : ExpressionNode() {
    override val lvalue = false
    override val type by lazy {
        val (t, f) = listOf(trueExpression.type, falseExpression.type)
        when (condition.type) {
            is UnknownType -> UnknownType
            is BoolType -> when {
                t is UnknownType || f is UnknownType -> UnknownType
                t == f -> t
                else -> {
                    println("types of two alternatives do not match")
                    UnknownType
                }
            }
            else -> {
                println("type of condition must be $BoolType")
                UnknownType
            }
        }
    }
}

class IdentifierExpressionNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val name: ExpressionNode
) : ExpressionNode()

sealed class ConstantNode : ExpressionNode()

class IntConstantNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val value: Int
) : ConstantNode()

class TypeNode(
    override val location: Location,
    val type: Type
) : ASTNode()
