package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.ASTException
import personal.wuqing.mxcompiler.astErrorInfo
import personal.wuqing.mxcompiler.frontend.ArrayType
import personal.wuqing.mxcompiler.frontend.BoolType
import personal.wuqing.mxcompiler.frontend.IntType
import personal.wuqing.mxcompiler.frontend.NullType
import personal.wuqing.mxcompiler.frontend.StringType
import personal.wuqing.mxcompiler.frontend.Type
import personal.wuqing.mxcompiler.frontend.UnknownType
import personal.wuqing.mxcompiler.frontend.VoidType
import personal.wuqing.mxcompiler.utils.Location
import java.io.Serializable

sealed class ASTNode : Serializable {
    abstract val location: Location
    abstract val summary: String
}

class ProgramNode(
    override val location: Location,
    val declarations: List<DeclarationNode>
) : ASTNode() {
    override val summary = "(Program)"
}

sealed class DeclarationNode : ASTNode()

class ParameterNode(
    override val location: Location, val name: String,
    val type: TypeNode
) : ASTNode() {
    override val summary = "$name (Parameter)"
}

class FunctionDeclarationNode(
    override val location: Location, val name: String,
    val returnType: TypeNode, val parameterList: List<ParameterNode>, val body: BlockNode
) : DeclarationNode() {
    override val summary = "$name (Function)"
}

class VariableDeclarationListNode(
    override val location: Location,
    val list: List<VariableDeclarationNode>
) : ASTNode() {
    override val summary: String
        get() = throw ASTException() // should not be on AST
}

class VariableDeclarationNode(
    override val location: Location, val name: String,
    val type: TypeNode, val init: ExpressionNode?
) : DeclarationNode() {
    override val summary = "$name (VariableDeclaration)"
}

class ClassDeclarationNode(
    override val location: Location, val name: String,
    val variables: List<VariableDeclarationNode>, val functions: List<FunctionDeclarationNode>
) : DeclarationNode() {
    override val summary = "$name (ClassDeclaration)"
}

sealed class StatementNode : ASTNode()

class EmptyStatementNode(
    override val location: Location
) : StatementNode() {
    override val summary = "(EmptyStatement)"
}

class BlockNode(
    override val location: Location,
    val statements: List<StatementNode>
) : StatementNode() {
    override val summary = "(Block)"
}

class ExpressionStatementNode(
    override val location: Location,
    val expression: ExpressionNode
) : StatementNode() {
    override val summary = "(Expression)"
}

class VariableDeclarationStatementNode(
    override val location: Location,
    val variables: List<VariableDeclarationNode>
) : StatementNode() {
    override val summary = "(VariableDeclaration)"
}

class IfNode(
    override val location: Location,
    val condition: ExpressionNode, val thenStatement: StatementNode, val elseStatement: StatementNode?
) : StatementNode() {
    override val summary = "(If)"
}

class WhileNode(
    override val location: Location,
    val condition: ExpressionNode, val statement: StatementNode
) : StatementNode() {
    override val summary = "(While)"
}

class ForNode(
    override val location: Location,
    val initVariableDeclaration: List<VariableDeclarationNode>, val initExpression: ExpressionNode?,
    val condition: ExpressionNode, val step: ExpressionNode?, val statement: StatementNode
) : StatementNode() {
    override val summary = "(For)"
}

class ContinueNode(
    override val location: Location
) : StatementNode() {
    override val summary = "(Continue)"
}

class BreakNode(
    override val location: Location
) : StatementNode() {
    override val summary = "(Break)"
}

class ReturnNode(
    override val location: Location,
    val expression: ExpressionNode?
) : StatementNode() {
    override val summary = "(Return)"
}

sealed class ExpressionNode : ASTNode() {
    abstract val type: Type
    abstract val lvalue: Boolean
}

class NewOperatorNode(
    override val location: Location,
    val baseType: TypeNode, val dimension: Int, val length: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = true
    override val type by lazy {
        if (baseType.type is UnknownType) UnknownType
        else ArrayType(baseType.type)
    }
    override val summary = "$dimension-dimension (New)"
}

class MemberAccessNode(
    override val location: Location,
    val parent: ExpressionNode, private val child: String
) : ExpressionNode() {
    override val lvalue = true
    override val type by lazy {
        if (parent.type is UnknownType) UnknownType
        else parent.type.variables[child]?.type ?: {
            println(astErrorInfo(location, "unknown member $child of $parent"))
            UnknownType
        }()
    }
    override val summary = "$child (MemberAccess)"
}

class ExpressionListNode(
    override val location: Location,
    val list: List<ExpressionNode>
) : ASTNode() {
    override val summary: String
        get() = throw ASTException() // should not be on AST
}

class MemberFunctionCallNode(
    override val location: Location,
    val parent: ExpressionNode, private val name: String, val parameters: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = false
    override val type by lazy {
        UnknownType // TODO: function call return type
    }
    override val summary = "$name (MemberFunctionCall)"
}

class FunctionCallNode(
    override val location: Location,
    private val name: String, val parameters: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = false
    override val type by lazy {
        UnknownType // TODO: function call return type
    }
    override val summary = "$name (FunctionCall)"
}

class IndexAccessNode(
    override val location: Location,
    val parent: ExpressionNode, val child: ExpressionNode
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
    override val summary = "(IndexAccess)"
}

class SuffixUnaryNode(
    override val location: Location,
    val operand: ExpressionNode, private val operator: SuffixOperator
) : ExpressionNode() {
    enum class SuffixOperator(private val displayText: String) {
        INC("++"), DEC("--");

        override fun toString() = displayText
    }

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

    override val summary = "'$operator' (SuffixOperator)"
}

class PrefixUnaryNode(
    override val location: Location,
    val operand: ExpressionNode, private val operator: PrefixOperator
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

    override val summary = "'$operator' (PrefixOperator)"
}

class BinaryNode(
    override val location: Location,
    val lhs: ExpressionNode, val rhs: ExpressionNode, private val operator: BinaryOperator
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

    override val summary = "'$operator' (BinaryOperator)"
}

class TernaryNode(
    override val location: Location,
    val condition: ExpressionNode,
    val thenExpression: ExpressionNode, val elseExpression: ExpressionNode
) : ExpressionNode() {
    override val lvalue = false
    override val type by lazy {
        val (t, f) = listOf(thenExpression.type, elseExpression.type)
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

    override val summary = "(TernaryOperator)"
}

class IdentifierExpressionNode(
    override val location: Location,
    val name: String
) : ExpressionNode() {
    override val lvalue = true
    override val type by lazy {
        UnknownType // TODO: identifier type
    }

    override val summary = "$name (Identifier)"
}

sealed class ConstantNode : ExpressionNode()

class IntConstantNode(
    override val location: Location,
    val value: Int
) : ConstantNode() {
    override val lvalue = false
    override val type = IntType

    override val summary = "$value (IntConstant)"
}

class StringConstantNode(
    override val location: Location,
    val value: String
) : ConstantNode() {
    override val lvalue = false
    override val type = StringType

    override val summary = "'$value' (StringConstant)"
}

class TrueConstantNode(
    override val location: Location
) : ConstantNode() {
    override val lvalue = false
    override val type = BoolType

    override val summary = "True (BoolConstant)"
}

class FalseConstantNode(
    override val location: Location
) : ConstantNode() {
    override val lvalue = false
    override val type = BoolType

    override val summary = "False (BoolConstant)"
}

class NullConstantNode(
    override val location: Location
) : ConstantNode() {
    override val lvalue = false
    override val type = NullType

    override val summary = "Null (NullConstant)"
}

sealed class TypeNode : ASTNode() {
    abstract val type: Type
}

class SimpleTypeNode(
    override val location: Location,
    val name: String
) : TypeNode() {
    override val type by lazy {
        UnknownType // TODO: type node
    }

    override val summary = "$name (SimpleType)"
}

class ArrayTypeNode(
    override val location: Location,
    val name: String, val dimension: Int
) : TypeNode() {
    override val type by lazy {
        UnknownType // TODO: type node
    }

    override val summary = "$name $dimension (ArrayType)"
}
