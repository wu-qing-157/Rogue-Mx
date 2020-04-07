package personal.wuqing.rogue.ast

import personal.wuqing.rogue.grammar.operator.MxBinary
import personal.wuqing.rogue.grammar.operator.MxPrefix
import personal.wuqing.rogue.grammar.operator.MxSuffix
import personal.wuqing.rogue.parser.MxLangBaseVisitor
import personal.wuqing.rogue.parser.MxLangParser
import personal.wuqing.rogue.utils.ASTErrorRecorder
import personal.wuqing.rogue.utils.Location
import kotlin.system.exitProcess

class ASTBuilder(private val filename: String) : MxLangBaseVisitor<ASTNode>() {
    class Exception(message: String) : kotlin.Exception(message)

    override fun visitProgram(ctx: MxLangParser.ProgramContext?) =
        ASTNode.Program(
            location = Location(filename, ctx!!),
            declarations = ctx.section().map {
                when (val child = visit(it)) {
                    is ASTNode.Declaration.Function, is ASTNode.Declaration.Class -> listOf(child)
                    is ASTNode.Declaration.VariableList -> child.list
                    else -> listOf()
                }
            }.flatten().map { it as ASTNode.Declaration }
        )

    override fun visitSection(ctx: MxLangParser.SectionContext?) =
        when {
            ctx!!.functionDeclaration() != null -> visit(ctx.functionDeclaration())!!
            ctx.classDeclaration() != null -> visit(ctx.classDeclaration())!!
            ctx.variableDeclaration() != null -> visit(ctx.variableDeclaration())!!
            else -> throw Exception("unknown section when building AST")
        }

    override fun visitBlock(ctx: MxLangParser.BlockContext?) =
        ASTNode.Statement.Block(
            location = Location(filename, ctx!!),
            statements = ctx.statement()?.map { visit(it) as ASTNode.Statement } ?: listOf()
        )

    override fun visitFunctionDeclaration(ctx: MxLangParser.FunctionDeclarationContext?) =
        ASTNode.Declaration.Function(
            location = Location(filename, ctx!!),
            name = ctx.Identifier()?.text ?: "<unknown>",
            result = visit(ctx.type()) as ASTNode.Type,
            parameterList = ctx.parameter().map {
                ASTNode.Declaration.Variable(
                    location = Location(filename, it),
                    type = visit(it.type()) as ASTNode.Type,
                    name = it.Identifier()?.text ?: "<unknown>",
                    init = null
                )
            },
            body = visit(ctx.block()) as ASTNode.Statement.Block
        )

    override fun visitConstructorDeclaration(ctx: MxLangParser.ConstructorDeclarationContext?) =
        ASTNode.Declaration.Constructor(
            location = Location(filename, ctx!!),
            type = visit(ctx.type()) as ASTNode.Type,
            parameterList = ctx.parameter().map {
                ASTNode.Declaration.Variable(
                    location = Location(filename, it),
                    type = visit(it.type()) as ASTNode.Type,
                    name = it.Identifier()?.text ?: "<unknown>",
                    init = null
                )
            },
            body = visit(ctx.block()) as ASTNode.Statement.Block
        )

    override fun visitClassDeclaration(ctx: MxLangParser.ClassDeclarationContext?) =
        ASTNode.Declaration.Class(
            location = Location(filename, ctx!!),
            name = ctx.Identifier()?.text ?: "<unknown>",
            declarations = ctx.classMember().map {
                when (val child = visit(it)) {
                    is ASTNode.Declaration.Function, is ASTNode.Declaration.Constructor -> listOf(child)
                    is ASTNode.Declaration.VariableList -> child.list
                    else -> listOf()
                }
            }.flatten().map { it as ASTNode.Declaration }
        )

    override fun visitVariableDeclaration(ctx: MxLangParser.VariableDeclarationContext?) =
        ASTNode.Declaration.VariableList(
            location = Location(filename, ctx!!),
            list = ctx.variable().map {
                ASTNode.Declaration.Variable(
                    location = Location(filename, it),
                    type = visit(ctx.type()) as ASTNode.Type,
                    name = it.Identifier()?.text ?: "<unknown>",
                    init = it.expression()?.let { expression ->
                        visit(expression) as? ASTNode.Expression ?: throw Exception("null expression")
                    }
                )
            }
        )

    override fun visitEmptyStatement(ctx: MxLangParser.EmptyStatementContext?) =
        ASTNode.Statement.Empty(
            location = Location(filename, ctx!!)
        )

    override fun visitBlockStatement(ctx: MxLangParser.BlockStatementContext?) =
        ASTNode.Statement.Block(
            location = Location(filename, ctx!!),
            statements = ctx.block().statement().map { visit(it) as ASTNode.Statement }
        )

    override fun visitExpressionStatement(ctx: MxLangParser.ExpressionStatementContext?) =
        ASTNode.Statement.Expression(
            location = Location(filename, ctx!!),
            expression = visit(ctx.expression()) as ASTNode.Expression
        )

    override fun visitVariableDeclarationStatement(ctx: MxLangParser.VariableDeclarationStatementContext?) =
        ASTNode.Statement.Variable(
            location = Location(filename, ctx!!),
            variables = (visit(ctx.variableDeclaration()) as ASTNode.Declaration.VariableList).list
        )

    override fun visitReturnStatement(ctx: MxLangParser.ReturnStatementContext?) =
        ASTNode.Statement.Return(
            location = Location(filename, ctx!!),
            expression = ctx.expression()?.let { visit(it) as ASTNode.Expression }
        )

    override fun visitBreakStatement(ctx: MxLangParser.BreakStatementContext?) =
        ASTNode.Statement.Break(Location(filename, ctx!!))

    override fun visitContinueStatement(ctx: MxLangParser.ContinueStatementContext?) =
        ASTNode.Statement.Continue(Location(filename, ctx!!))

    override fun visitIfStatement(ctx: MxLangParser.IfStatementContext?) =
        ASTNode.Statement.If(
            location = Location(filename, ctx!!),
            condition = visit(ctx.expression()) as ASTNode.Expression,
            then = visit(ctx.statement()) as ASTNode.Statement,
            els = null
        )

    override fun visitIfElseStatement(ctx: MxLangParser.IfElseStatementContext?) =
        ASTNode.Statement.If(
            location = Location(filename, ctx!!),
            condition = visit(ctx.expression()) as ASTNode.Expression,
            then = visit(ctx.thenStatement) as ASTNode.Statement,
            els = visit(ctx.elseStatement) as ASTNode.Statement
        )

    override fun visitWhileStatement(ctx: MxLangParser.WhileStatementContext?) =
        ASTNode.Statement.Loop.While(
            location = Location(filename, ctx!!),
            condition = visit(ctx.expression()) as ASTNode.Expression,
            statement = visit(ctx.statement()) as ASTNode.Statement
        )

    override fun visitForStatement(ctx: MxLangParser.ForStatementContext?) =
        ASTNode.Statement.Loop.For(
            location = Location(filename, ctx!!),
            init = when {
                ctx.initExpression != null ->
                    ASTNode.Statement.Expression(
                        location = Location(filename, ctx),
                        expression = visit(ctx.initExpression) as ASTNode.Expression
                    )
                ctx.initVariableDeclaration != null ->
                    ASTNode.Statement.Variable(
                        location = Location(filename, ctx),
                        variables = (visit(ctx.variableDeclaration()) as ASTNode.Declaration.VariableList).list
                    ).also { exitProcess(12) }
                else -> null
            },
            condition = ctx.condition?.let { visit(it) as ASTNode.Expression } ?: ASTNode.Expression.Constant.True(
                Location(filename, ctx)
            ),
            step = ctx.step?.let { visit(it) as ASTNode.Expression },
            statement = visit(ctx.statement()) as ASTNode.Statement
        )

    override fun visitParentheses(ctx: MxLangParser.ParenthesesContext?) = visit(ctx!!.expression())!!

    override fun visitNewObject(ctx: MxLangParser.NewObjectContext?) =
        ASTNode.Expression.NewObject(
            location = Location(filename, ctx!!),
            baseType = visit(ctx.simpleType()) as ASTNode.Type,
            parameters = ctx.expressionList()?.let {
                (visit(it) as ASTNode.Expression.ExpressionList).list
            } ?: listOf()
        )

    override fun visitNewArray(ctx: MxLangParser.NewArrayContext?) =
        ASTNode.Expression.NewArray(
            location = Location(filename, ctx!!),
            baseType = visit(ctx.simpleType()) as ASTNode.Type,
            dimension = ctx.numberedBracket().size,
            length = ctx.numberedBracket().map { it.expression() }.run {
                when {
                    get(0) == null -> listOf<ASTNode.Expression>().also {
                        ASTErrorRecorder.error(Location(filename, ctx), "length of array not specified")
                    }
                    indexOfFirst { it == null }.let { s ->
                        s != -1 && subList(s, size).any { it != null }
                    } -> listOf<ASTNode.Expression>().also {
                        ASTErrorRecorder.error(
                            Location(filename, ctx), "length of dimensions should be specified from left to right"
                        )
                    }
                    else -> filterNotNull().map { visit(it) as ASTNode.Expression }
                }
            }
        )

    override fun visitExpressionList(ctx: MxLangParser.ExpressionListContext?) =
        ASTNode.Expression.ExpressionList(
            location = Location(filename, ctx!!),
            list = ctx.expression().map { visit(it) as ASTNode.Expression }
        )

    override fun visitMemberAccess(ctx: MxLangParser.MemberAccessContext?) =
        ASTNode.Expression.MemberAccess(
            location = Location(filename, ctx!!),
            parent = visit(ctx.expression()) as ASTNode.Expression,
            child = ctx.Identifier()?.text ?: "<unknown>"
        )

    override fun visitMemberFunctionCall(ctx: MxLangParser.MemberFunctionCallContext?) =
        ASTNode.Expression.MemberFunction(
            location = Location(filename, ctx!!),
            base = visit(ctx.expression()) as ASTNode.Expression,
            name = ctx.Identifier()?.text ?: "<unknown>",
            parameters = (visit(ctx.expressionList()) as ASTNode.Expression.ExpressionList).list
        )

    override fun visitFunctionCall(ctx: MxLangParser.FunctionCallContext?) =
        ASTNode.Expression.Function(
            location = Location(filename, ctx!!),
            name = ctx.Identifier()?.text ?: "<unknown>",
            parameters = (visit(ctx.expressionList()) as ASTNode.Expression.ExpressionList).list
        )

    override fun visitIndexAccess(ctx: MxLangParser.IndexAccessContext?) =
        ASTNode.Expression.Index(
            location = Location(filename, ctx!!),
            parent = visit(ctx.expression(0)) as ASTNode.Expression,
            child = visit(ctx.expression(1)) as ASTNode.Expression
        )

    override fun visitSuffixUnaryOperator(ctx: MxLangParser.SuffixUnaryOperatorContext?) =
        ASTNode.Expression.Suffix(
            location = Location(filename, ctx!!),
            operand = visit(ctx.expression()) as ASTNode.Expression,
            operator = when (ctx.op.text) {
                "++" -> MxSuffix.INC
                "--" -> MxSuffix.DEC
                else -> throw Exception("unknown suffix unary operator when building AST")
            }
        )

    override fun visitPrefixUnaryOperator(ctx: MxLangParser.PrefixUnaryOperatorContext?) =
        ASTNode.Expression.Prefix(
            location = Location(filename, ctx!!),
            operand = visit(ctx.expression()) as ASTNode.Expression,
            operator = when (ctx.op.text) {
                "++" -> MxPrefix.INC
                "--" -> MxPrefix.DEC
                "+" -> MxPrefix.POS
                "-" -> MxPrefix.NEG
                "!" -> MxPrefix.L_NEG
                "~" -> MxPrefix.INV
                else -> throw Exception("unknown prefix unary operator when building AST")
            }
        )

    override fun visitBinaryOperator(ctx: MxLangParser.BinaryOperatorContext?) =
        ASTNode.Expression.Binary(
            location = Location(filename, ctx!!),
            lhs = visit(ctx.expression(0)) as ASTNode.Expression,
            rhs = visit(ctx.expression(1)) as ASTNode.Expression,
            operator = when (ctx.op.text) {
                "*" -> MxBinary.TIMES
                "/" -> MxBinary.DIV
                "%" -> MxBinary.REM
                "+" -> MxBinary.PLUS
                "-" -> MxBinary.MINUS
                "<<" -> MxBinary.SHL
                ">>" -> MxBinary.SHR
                ">>>" -> MxBinary.U_SHR
                "<" -> MxBinary.LESS
                ">" -> MxBinary.GREATER
                "<=" -> MxBinary.LEQ
                ">=" -> MxBinary.GEQ
                "==" -> MxBinary.EQUAL
                "!=" -> MxBinary.NEQ
                "&" -> MxBinary.A_AND
                "^" -> MxBinary.A_XOR
                "|" -> MxBinary.A_OR
                "&&" -> MxBinary.L_AND
                "||" -> MxBinary.L_OR
                "=" -> MxBinary.ASSIGN
                "+=" -> MxBinary.PLUS_I
                "-=" -> MxBinary.MINUS_I
                "*=" -> MxBinary.TIMES_I
                "/=" -> MxBinary.DIV_I
                "%=" -> MxBinary.REM_I
                "&=" -> MxBinary.AND_I
                "^=" -> MxBinary.XOR_I
                "|=" -> MxBinary.OR_I
                "<<=" -> MxBinary.SHL_I
                ">>=" -> MxBinary.SHR_I
                ">>>=" -> MxBinary.U_SHR_I
                else -> throw Exception("unknown binary operator when building AST")
            }
        )

    override fun visitTernaryOperator(ctx: MxLangParser.TernaryOperatorContext?) =
        ASTNode.Expression.Ternary(
            location = Location(filename, ctx!!),
            condition = visit(ctx.expression(0)) as ASTNode.Expression,
            then = visit(ctx.expression(1)) as ASTNode.Expression,
            els = visit(ctx.expression(2)) as ASTNode.Expression
        )

    override fun visitThisExpression(ctx: MxLangParser.ThisExpressionContext?) =
        ASTNode.Expression.This(
            location = Location(filename, ctx!!)
        )

    override fun visitIdentifiers(ctx: MxLangParser.IdentifiersContext?) =
        ASTNode.Expression.Identifier(
            location = Location(filename, ctx!!),
            name = ctx.Identifier()?.text ?: "<unknown>"
        )

    override fun visitConstants(ctx: MxLangParser.ConstantsContext?) =
        visit(ctx!!.constant()) as ASTNode.Expression

    override fun visitConstant(ctx: MxLangParser.ConstantContext?) =
        when {
            ctx!!.IntConstant() != null -> ASTNode.Expression.Constant.Int(
                location = Location(filename, ctx),
                value = ctx.IntConstant().text.run {
                    try {
                        toInt()
                    } catch (e: NumberFormatException) {
                        ASTErrorRecorder.error(Location(filename, ctx), "invalid integer constant \"$this\"")
                        0
                    }
                }
            )
            ctx.StringConstant() != null -> ASTNode.Expression.Constant.String(
                location = Location(filename, ctx),
                value = ctx.StringConstant().text
                    .removeSurrounding("\"")
                    .replace(Regex("\\\\(([\\\\benrt\"])|u([0-9a-fA-F]{4}))")) {
                        when (it.groupValues[2]) {
                            "t" -> "\t"
                            "b" -> "\b"
                            "e" -> "\u001b"
                            "r" -> "\r"
                            "n" -> "\n"
                            "\"" -> "\""
                            "\\" -> "\\"
                            else -> it.groupValues[3].toInt(16).toChar().toString()
                        }
                    }
            )
            ctx.Null() != null -> ASTNode.Expression.Constant.Null(Location(filename, ctx))
            ctx.True() != null -> ASTNode.Expression.Constant.True(Location(filename, ctx))
            ctx.False() != null -> ASTNode.Expression.Constant.False(Location(filename, ctx))
            else -> throw Exception("unknown constant value when building AST")
        }

    override fun visitType(ctx: MxLangParser.TypeContext?) =
        when {
            ctx!!.simpleType() != null -> visitSimpleType(ctx.simpleType())
            ctx.arrayType() != null -> visitArrayType(ctx.arrayType())
            else -> throw Exception("unknown type when building AST")
        }

    override fun visitSimpleType(ctx: MxLangParser.SimpleTypeContext?) =
        ASTNode.Type.Simple(
            location = Location(filename, ctx!!),
            name = ctx.text
        )

    override fun visitArrayType(ctx: MxLangParser.ArrayTypeContext?) =
        ASTNode.Type.Array(
            location = Location(filename, ctx!!),
            name = ctx.simpleType().text,
            dimension = ctx.bracket().size
        )
}
