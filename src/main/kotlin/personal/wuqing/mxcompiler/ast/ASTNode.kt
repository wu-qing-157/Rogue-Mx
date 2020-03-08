package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.grammar.operator.MxBinary
import personal.wuqing.mxcompiler.grammar.operator.MxPrefix
import personal.wuqing.mxcompiler.grammar.operator.MxSuffix
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.grammar.MxFunction as Function_
import personal.wuqing.mxcompiler.grammar.MxType as Type_
import personal.wuqing.mxcompiler.grammar.MxType.Class as Class_
import personal.wuqing.mxcompiler.grammar.MxVariable as Variable_

sealed class ASTNode {
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
            lateinit var actual: Function_ private set
            fun init(function: Function_) {
                actual = function
            }
        }

        class Constructor(
            location: Location, type: Type, parameterList: List<Variable>, body: Statement.Block
        ) : Function(location, "__constructor__", type, parameterList, body) {
            override val summary get() = "(Constructor)"
            override val returnType = Type_.Void
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
            lateinit var actual: Variable_ private set
            fun init(variable: Variable_) {
                actual = variable
            }
        }

        class Class(
            override val location: Location, val name: String,
            val declarations: List<Declaration>
        ) : Declaration() {
            override val summary get() = "$name (ClassDeclaration)"
            val actual = Class_(name, this)
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
            override val location: Location, val condition: ASTNode.Expression, val then: Statement, val els: Statement?
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
                val init: Statement?, val condition: ASTNode.Expression, val step: ASTNode.Expression?,
                val statement: Statement
            ) : Loop() {
                override val summary get() = "(For)"
            }
        }

        class Continue(
            override val location: Location
        ) : Statement() {
            override val summary get() = "(Continue)"
            lateinit var loop: Loop private set
            fun init(loop: Loop) {
                this.loop = loop
            }
        }

        class Break(
            override val location: Location
        ) : Statement() {
            override val summary get() = "(Break)"
            lateinit var loop: Loop private set
            fun init(loop: Loop) {
                this.loop = loop
            }
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
            override val location: Location, val baseType: Type, val dimension: Int, val length: List<Expression>
        ) : Expression() {
            override val summary get() = "$dimension-dimension (New Array)"
            override val type by type()
            override val lvalue = false
        }

        class MemberAccess(
            override val location: Location, val parent: Expression, val child: String
        ) : Expression() {
            override val summary get() = "$child (MemberAccess)"
            val resolved by resolve()
            val reference get() = resolved!!
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
            val resolved by resolve()
            val reference get() = resolved!!
            override val type by type()
            override val lvalue = false
        }

        class Function(
            override val location: Location, val name: String, val parameters: List<Expression>
        ) : Expression() {
            override val summary get() = "$name (FunctionCall)"
            val resolved by resolve()
            val reference get() = resolved!!
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
            override val location: Location, val operand: Expression, val operator: MxSuffix
        ) : Expression() {
            override val summary get() = "'$operator' (SuffixOperator)"
            override val type by type()
            override val lvalue = false
        }

        class Prefix(
            override val location: Location, val operand: Expression, val operator: MxPrefix
        ) : Expression() {
            override val summary get() = "'$operator' (PrefixOperator)"
            override val type by type()
            override val lvalue = operator.lvalue
        }

        class Binary(
            override val location: Location, val lhs: Expression, val rhs: Expression, val operator: MxBinary
        ) : Expression() {
            override val summary get() = "'$operator' (BinaryOperator)"
            override val type by type()
            override val lvalue = operator.lvalue
        }

        class Ternary(
            override val location: Location, val condition: Expression,
            val then: Expression, val els: Expression
        ) : Expression() {
            override val summary get() = "(TernaryOperator)"
            override val type by type()
            override val lvalue = then.lvalue && els.lvalue
        }

        class Identifier(
            override val location: Location, val name: String
        ) : Expression() {
            override val summary get() = "$name (Identifier)"
            val resolved by resolve()
            val reference get() = resolved!!
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
                override val type = Type_.Primitive.Int
            }

            class String(
                override val location: Location, val value: kotlin.String
            ) : Constant() {
                override val summary get() = "'$value' (StringConstant)"
                override val type = Type_.Primitive.String
            }

            class True(
                override val location: Location
            ) : Constant() {
                override val summary get() = "True (BoolConstant)"
                override val type = Type_.Primitive.Bool
            }

            class False(
                override val location: Location
            ) : Constant() {
                override val summary get() = "False (BoolConstant)"
                override val type = Type_.Primitive.Bool
            }

            class Null(
                override val location: Location
            ) : Constant() {
                override val summary get() = "Null (NullConstant)"
                override val type = Type_.Null
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
