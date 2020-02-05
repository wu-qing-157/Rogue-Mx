package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.ASTException
import personal.wuqing.mxcompiler.frontend.BoolType
import personal.wuqing.mxcompiler.frontend.IntType
import personal.wuqing.mxcompiler.frontend.NullType
import personal.wuqing.mxcompiler.frontend.StringType
import personal.wuqing.mxcompiler.frontend.Type
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

class ConstructorDeclarationNode(
    override val location: Location,
    val type: TypeNode, val parameterList: List<ParameterNode>, val body: BlockNode
) : DeclarationNode() {
    override val summary = "$type (Constructor)"
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
    val variables: List<VariableDeclarationNode>, val functions: List<FunctionDeclarationNode>,
    val constructors: List<ConstructorDeclarationNode>
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

class NewObjectNode(
    override val location: Location,
    val baseType: TypeNode, val parameters: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = false
    override val type by type()
    override val summary = "$baseType (New Object)"
}

class NewArrayNode(
    override val location: Location,
    val baseType: TypeNode, val dimension: Int, val length: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = true
    override val type by type()
    override val summary = "$dimension-dimension (New Array)"
}

class MemberAccessNode(
    override val location: Location,
    val parent: ExpressionNode, val child: String
) : ExpressionNode() {
    override val lvalue = true
    override val type by type()
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
    override val type by type()
    override val summary = "$name (MemberFunctionCall)"
}

class FunctionCallNode(
    override val location: Location,
    private val name: String, val parameters: List<ExpressionNode>
) : ExpressionNode() {
    override val lvalue = false
    override val type by type()
    override val summary = "$name (FunctionCall)"
}

class IndexAccessNode(
    override val location: Location,
    val parent: ExpressionNode, val child: ExpressionNode
) : ExpressionNode() {
    override val lvalue = true
    override val type by type()
    override val summary = "(IndexAccess)"
}

class SuffixUnaryNode(
    override val location: Location,
    val operand: ExpressionNode, val operator: SuffixOperator
) : ExpressionNode() {
    enum class SuffixOperator(private val displayText: String) {
        INC("++"), DEC("--");

        override fun toString() = displayText
    }

    override val lvalue = false
    override val type by type()
    override val summary = "'$operator' (SuffixOperator)"
}

class PrefixUnaryNode(
    override val location: Location,
    val operand: ExpressionNode, val operator: PrefixOperator
) : ExpressionNode() {
    enum class PrefixOperator(private val displayText: String, val lvalue: Boolean) {
        INC("++", true), DEC("--", true),
        LOGIC_NEGATION("!", false), ARITHMETIC_NEGATION("~", false),
        POSITIVE("+", false), NEGATIVE("-", false);

        override fun toString() = displayText
    }

    override val lvalue = operator.lvalue
    override val type by type()
    override val summary = "'$operator' (PrefixOperator)"
}

class BinaryNode(
    override val location: Location,
    val lhs: ExpressionNode, val rhs: ExpressionNode, val operator: BinaryOperator
) : ExpressionNode() {
    enum class BinaryOperator(private val displayText: String, val lvalue: Boolean) {
        PLUS("+", false), MINUS("-", false),
        TIMES("*", false), DIVIDE("/", false), REM("%", false),
        LOGIC_AND("&&", false), LOGIC_OR("||", false),
        ARITHMETIC_AND("&", false), ARITHMETIC_OR("|", false),
        ARITHMETIC_XOR("^", false),
        SHIFT_LEFT("<<", false),
        LOGIC_SHIFT_RIGHT(">>>", false), ARITHMETIC_SHIFT_RIGHT(">>", false),
        LESS("<", false), LESS_EQUAL("<=", false),
        GREATER(">", false), GREATER_EQUAL(">=", false),
        EQUAL("==", false), NOT_EQUAL("!=", false),
        ASSIGN("=", true),
        PLUS_ASSIGN("+=", true), MINUS_ASSIGN("-=", true),
        TIMES_ASSIGN("*=", true), DIVIDE_ASSIGN("/=", true),
        REM_ASSIGN("%=", true),
        AND_ASSIGN("&=", true), OR_ASSIGN("|=", true),
        XOR_ASSIGN("^=", true),
        SHIFT_LEFT_ASSIGN("<<=", true),
        LOGIC_SHIFT_RIGHT_ASSIGN(">>>=", true),
        ARITHMETIC_SHIFT_RIGHT_ASSIGN(">>=", true);

        override fun toString() = displayText
    }

    override val lvalue = operator.lvalue
    override val type by type()
    override val summary = "'$operator' (BinaryOperator)"
}

class TernaryNode(
    override val location: Location,
    val condition: ExpressionNode,
    val thenExpression: ExpressionNode, val elseExpression: ExpressionNode
) : ExpressionNode() {
    override val lvalue = false
    override val type by type()
    override val summary = "(TernaryOperator)"
}

class IdentifierExpressionNode(
    override val location: Location,
    val name: String
) : ExpressionNode() {
    override val lvalue = true
    override val type by type()
    override val summary = "$name (Identifier)"
}

class ThisExpressionNode(
    override val location: Location
) : ExpressionNode() {
    override val lvalue = false
    override val type by type()
    override val summary = "(This)"
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
    override val type by type()
    override val summary = "$name (SimpleType)"
}

class ArrayTypeNode(
    override val location: Location,
    val name: String, val dimension: Int
) : TypeNode() {
    override val type by type()
    override val summary = "$name $dimension (ArrayType)"
}
