package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.type.Type
import personal.wuqing.mxcompiler.utils.Location

sealed class ASTNode {
    abstract val location: Location
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
    override val type: Type, override val lvalue: Boolean,
    val parent: ExpressionNode, val child: String
) : ExpressionNode()

class NewOperatorNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val baseType: TypeNode
) : ExpressionNode()

class FunctionCallNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val function: FunctionDeclarationNode, val parameters: List<ExpressionNode>
) : ExpressionNode()

class IndexAccessNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val parent: ExpressionNode, val child: ExpressionNode
) : ExpressionNode()

class SuffixUnaryNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val operand: ExpressionNode, val operator: SuffixOperator
) : ExpressionNode() {
    enum class SuffixOperator { INC, DEC }
}

class PrefixUnaryNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val operand: ExpressionNode, val operator: PrefixOperator
) : ExpressionNode() {
    enum class PrefixOperator { INC, DEC, LOGIC_NEGATION, ARITHMETIC_NEGATION, POSITIVE, NEGATIVE }
}

class BinaryNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val lhs: ExpressionNode, val rhs: ExpressionNode, val operator: BinaryOperator
) : ExpressionNode() {
    enum class BinaryOperator {
        PLUS, MINUS, TIMES, DIVIDE, REM,
        LOGIC_AND, LOGIC_OR,
        ARITHMETIC_AND, ARITHMETIC_OR, ARITHMETIC_XOR,
        SHIFT_LEFT, LOGIC_SHIFT_RIGHT, ARITHMETIC_SHIFT_RIGHT,
        LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL,
        ASSIGN,
        PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIVIDE_ASSIGN, REM_ASSIGN,
        AND_ASSIGN, OR_ASSIGN, XOR_ASSIGN,
        SHIFT_LEFT_ASSIGN, LOGIC_SHIFT_RIGHT_ASSIGN, ARITHMETIC_SHIFT_RIGHT_ASSIGN,
    }
}

class TernaryNode(
    override val location: Location,
    override val type: Type, override val lvalue: Boolean,
    val condition: ExpressionNode, val trueExpression: ExpressionNode, val falseExpression: ExpressionNode
) : ExpressionNode()

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
