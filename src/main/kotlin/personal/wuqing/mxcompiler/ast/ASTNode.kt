package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.grammar.BinaryOperator
import personal.wuqing.mxcompiler.grammar.BoolType
import personal.wuqing.mxcompiler.grammar.ClassType
import personal.wuqing.mxcompiler.grammar.IntType
import personal.wuqing.mxcompiler.grammar.NullType
import personal.wuqing.mxcompiler.grammar.PrefixOperator
import personal.wuqing.mxcompiler.grammar.StringType
import personal.wuqing.mxcompiler.grammar.SuffixOperator
import personal.wuqing.mxcompiler.grammar.VoidType
import personal.wuqing.mxcompiler.utils.Location
import java.io.Serializable
import personal.wuqing.mxcompiler.grammar.Type as Type_

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
        open class Function(
            override val location: Location, val name: String,
            val result: Type, val parameterList: List<Variable>, val body: Statement.Block
        ) : Declaration() {
            override val summary get() = "$name (Function)"
            open val returnType by lazy { result.type }
        }

        class Constructor(
            location: Location,
            type: Type, parameterList: List<Variable>, body: Statement.Block
        ) : Function(location, "<constructor>", type, parameterList, body) {
            override val summary get() = "(Constructor)"
            override val returnType = VoidType
        }

        class VariableList(
            override val location: Location, val list: List<Variable>
        ) : ASTNode() {
            override val summary: String get() = throw Exception("unexpected AST node") // should not be on AST
        }

        class Variable(
            override val location: Location, val name: String, val type: Type, val init: Expression?
        ) : Declaration() {
            override val summary get() = "$name (VariableDeclaration)"
        }

        class Class(
            override val location: Location, val name: String,
            val declarations: List<Declaration>
        ) : Declaration() {
            override val summary get() = "$name (ClassDeclaration)"
            val clazz = ClassType(name, this)
        }
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
            val condition: ASTNode.Expression, val then: Statement, val else_: Statement?
        ) : Statement() {
            override val summary get() = "(If)"
        }

        sealed class Loop : Statement() {
            class While(
                override val location: Location, val condition: ASTNode.Expression, val statement: Statement
            ) : Loop() {
                override val summary get() = "(While)"
            }

            class For(
                override val location: Location,
                val initVariable: List<Declaration.Variable>, val initExpression: ASTNode.Expression?,
                val condition: ASTNode.Expression, val step: ASTNode.Expression?, val statement: Statement
            ) : Loop() {
                override val summary get() = "(For)"
            }
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
        abstract val type: Type_
        abstract val lvalue: Boolean

        class NewObject(
            override val location: Location, val baseType: Type, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$baseType (New Object)"
            override val type by type()
            override val lvalue = false
        }

        class NewArray(
            override val location: Location, val baseType: Type, val dimension: Int, val length: List<Expression?>
        ) : Expression() {
            override val summary get() = "$dimension-dimension (New Array)"
            override val type by type()
            override val lvalue = false
        }

        class MemberAccess(
            override val location: Location, val parent: Expression, val child: String
        ) : Expression() {
            override val summary get() = "$child (MemberAccess)"
            override val type by type()
            override val lvalue = true
        }

        class ExpressionList(
            override val location: Location, val list: List<Expression>
        ) : ASTNode() {
            override val summary get() = throw Exception("unexpected AST node") // should not be on AST
        }

        class MemberFunction(
            override val location: Location, val base: Expression, val name: String, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$name (MemberFunctionCall)"
            override val type by type()
            override val lvalue = false
        }

        class Function(
            override val location: Location, val name: String, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$name (FunctionCall)"
            override val type by type()
            override val lvalue = false
        }

        class Index(
            override val location: Location, val parent: Expression, val child: Expression
        ) : Expression() {
            override val summary get() = "(IndexAccess)"
            override val type by type()
            override val lvalue = true
        }

        class Suffix(
            override val location: Location, val operand: Expression, val operator: SuffixOperator
        ) : Expression() {
            override val summary get() = "'$operator' (SuffixOperator)"
            override val type by type()
            override val lvalue = false
        }

        class Prefix(
            override val location: Location, val operand: Expression, val operator: PrefixOperator
        ) : Expression() {
            override val summary get() = "'$operator' (PrefixOperator)"
            override val type by type()
            override val lvalue = operator.lvalue
        }

        class Binary(
            override val location: Location, val lhs: Expression, val rhs: Expression, val operator: BinaryOperator
        ) : Expression() {
            override val summary get() = "'$operator' (BinaryOperator)"
            override val type by type()
            override val lvalue = operator.lvalue
        }

        class Ternary(
            override val location: Location, val condition: Expression,
            val then: Expression, val else_: Expression
        ) : Expression() {
            override val summary get() = "(TernaryOperator)"
            override val type by type()
            override val lvalue = then.lvalue && else_.lvalue
        }

        class Identifier(
            override val location: Location, val name: String
        ) : Expression() {
            override val summary get() = "$name (Identifier)"
            override val type by type()
            override val lvalue = true
        }

        class This(
            override val location: Location
        ) : Expression() {
            override val summary get() = "(This)"
            override val type by type()
            override val lvalue = false
        }

        sealed class Constant : Expression() {
            override val lvalue = false

            class Int(
                override val location: Location, val value: kotlin.Int
            ) : Constant() {
                override val summary get() = "$value (IntConstant)"
                override val type = IntType
            }

            class String(
                override val location: Location, val value: kotlin.String
            ) : Constant() {
                override val summary get() = "'$value' (StringConstant)"
                override val type = StringType
            }

            class True(
                override val location: Location
            ) : Constant() {
                override val summary get() = "True (BoolConstant)"
                override val type = BoolType
            }

            class False(
                override val location: Location
            ) : Constant() {
                override val summary get() = "False (BoolConstant)"
                override val type = BoolType
            }

            class Null(
                override val location: Location
            ) : Constant() {
                override val summary get() = "Null (NullConstant)"
                override val type = NullType
            }
        }
    }

    sealed class Type : ASTNode() {
        abstract val type: Type_

        class Simple(
            override val location: Location, val name: String
        ) : Type() {
            override val summary get() = "$name (SimpleType)"
            override val type by type()
        }

        class Array(
            override val location: Location, val name: String, val dimension: Int
        ) : Type() {
            override val summary get() = "$name $dimension (ArrayType)"
            override val type by type()
        }
    }
}
