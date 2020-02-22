package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.ASTException
import personal.wuqing.mxcompiler.frontend.BinaryOperator
import personal.wuqing.mxcompiler.frontend.PrefixOperator
import personal.wuqing.mxcompiler.frontend.SuffixOperator
import personal.wuqing.mxcompiler.utils.Location
import java.io.Serializable

sealed class ASTNode : Serializable {
    abstract val location: Location
    abstract val summary: String

    class Program(
        override val location: Location,
        val declarations: List<Declaration>
    ) : ASTNode() {
        override val summary get() = "(Program)"
    }

    sealed class Declaration : ASTNode() {
        class Function(
            override val location: Location, val name: String,
            val returnType: Type, val parameterList: List<Parameter>, val body: Statement.Block
        ) : Declaration() {
            override val summary get() = "$name (Function)"
        }

        class Constructor(
            override val location: Location,
            val type: Type, val parameterList: List<Parameter>, val body: Statement.Block
        ) : Declaration() {
            override val summary get() = "$type (Constructor)"
        }

        class VariableList(
            override val location: Location, val list: List<Variable>
        ) : ASTNode() {
            override val summary: String get() = throw ASTException() // should not be on AST
        }

        class Variable(
            override val location: Location, val name: String, val type: Type, val init: Expression?
        ) : Declaration() {
            override val summary get() = "$name (VariableDeclaration)"
        }

        class Class(
            override val location: Location, val name: String,
            val variables: List<Variable>, val functions: List<Function>, val constructors: List<Constructor>
        ) : Declaration() {
            override val summary get() = "$name (ClassDeclaration)"
        }
    }

    class Parameter(
        override val location: Location, val name: String, val type: Type
    ) : ASTNode() {
        override val summary get() = "$name (Parameter)"
    }

    sealed class Statement : ASTNode() {
        class Empty(
            override val location: Location
        ) : Statement() {
            override val summary get() = "(EmptyStatement)"
        }

        class Block(
            override val location: Location, val statements: List<Statement>
        ) : Statement() {
            override val summary get() = "(Block)"
        }

        class Expression(
            override val location: Location, val expression: ASTNode.Expression
        ) : Statement() {
            override val summary get() = "(Expression)"
        }

        class Variable(
            override val location: Location, val variables: List<Declaration.Variable>
        ) : Statement() {
            override val summary get() = "(VariableDeclaration)"
        }

        class If(
            override val location: Location,
            val condition: ASTNode.Expression, val thenStatement: Statement, val elseStatement: Statement?
        ) : Statement() {
            override val summary get() = "(If)"
        }

        class While(
            override val location: Location, val condition: ASTNode.Expression, val statement: Statement
        ) : Statement() {
            override val summary get() = "(While)"
        }

        class For(
            override val location: Location,
            val initVariableDeclaration: List<Declaration.Variable>, val initExpression: ASTNode.Expression?,
            val condition: ASTNode.Expression, val step: ASTNode.Expression?, val statement: Statement
        ) : Statement() {
            override val summary get() = "(For)"
        }

        class Continue(
            override val location: Location
        ) : Statement() {
            override val summary get() = "(Continue)"
        }

        class Break(
            override val location: Location
        ) : Statement() {
            override val summary get() = "(Break)"
        }

        class Return(
            override val location: Location, val expression: ASTNode.Expression?
        ) : Statement() {
            override val summary get() = "(Return)"
        }
    }

    sealed class Expression : ASTNode() {
        class NewObject(
            override val location: Location, val baseType: Type, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$baseType (New Object)"
        }

        class NewArray(
            override val location: Location, val baseType: Type, val dimension: Int, val length: List<Expression>
        ) : Expression() {
            override val summary get() = "$dimension-dimension (New Array)"
        }

        class MemberAccess(
            override val location: Location, val parent: Expression, val child: String
        ) : Expression() {
            override val summary get() = "$child (MemberAccess)"
        }

        class ExpressionList(
            override val location: Location, val list: List<Expression>
        ) : ASTNode() {
            override val summary: String get() = throw ASTException() // should not be on AST
        }

        class MemberFunction(
            override val location: Location, val base: Expression, val name: String, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$name (MemberFunctionCall)"
        }

        class Function(
            override val location: Location, val name: String, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$name (FunctionCall)"
        }

        class Index(
            override val location: Location, val parent: Expression, val child: Expression
        ) : Expression() {
            override val summary get() = "(IndexAccess)"
        }

        class Suffix(
            override val location: Location, val operand: Expression, val operator: SuffixOperator
        ) : Expression() {
            override val summary get() = "'$operator' (SuffixOperator)"
        }

        class Prefix(
            override val location: Location, val operand: Expression, val operator: PrefixOperator
        ) : Expression() {
            override val summary get() = "'$operator' (PrefixOperator)"
        }

        class Binary(
            override val location: Location, val lhs: Expression, val rhs: Expression, val operator: BinaryOperator
        ) : Expression() {
            override val summary get() = "'$operator' (BinaryOperator)"
        }

        class Ternary(
            override val location: Location, val condition: Expression,
            val thenExpression: Expression, val elseExpression: Expression
        ) : Expression() {
            override val summary get() = "(TernaryOperator)"
        }

        class Identifier(
            override val location: Location, val name: String
        ) : Expression() {
            override val summary get() = "$name (Identifier)"
        }

        class This(
            override val location: Location
        ) : Expression() {
            override val summary get() = "(This)"
        }

        sealed class Constant : Expression() {
            class Int(
                override val location: Location, val value: kotlin.Int
            ) : Constant() {
                override val summary get() = "$value (IntConstant)"
            }

            class String(
                override val location: Location, val value: kotlin.String
            ) : Constant() {
                override val summary get() = "'$value' (StringConstant)"
            }

            class True(
                override val location: Location
            ) : Constant() {
                override val summary get() = "True (BoolConstant)"
            }

            class False(
                override val location: Location
            ) : Constant() {
                override val summary get() = "False (BoolConstant)"
            }

            class Null(
                override val location: Location
            ) : Constant() {
                override val summary get() = "Null (NullConstant)"
            }
        }
    }

    sealed class Type : ASTNode() {
        class Simple(
            override val location: Location, val name: String
        ) : Type() {
            override val summary get() = "$name (SimpleType)"
        }

        class Array(
            override val location: Location, val name: String, val dimension: Int
        ) : Type() {
            override val summary get() = "$name $dimension (ArrayType)"
        }
    }
}
