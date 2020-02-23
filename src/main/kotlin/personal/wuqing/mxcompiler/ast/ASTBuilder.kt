package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.grammar.BinaryOperator
import personal.wuqing.mxcompiler.grammar.PrefixOperator
import personal.wuqing.mxcompiler.grammar.SuffixOperator
import personal.wuqing.mxcompiler.parser.MxLangBaseVisitor
import personal.wuqing.mxcompiler.parser.MxLangParser
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.Location

class ASTBuilder(private val filename: String) : MxLangBaseVisitor<ASTNode>() {
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
            name = ctx.Identifier().text,
            result = visit(ctx.type()) as ASTNode.Type,
            parameterList = ctx.parameter().map {
                ASTNode.Declaration.Variable(
                    location = Location(filename, it),
                    type = visit(it.type()) as ASTNode.Type,
                    name = it.Identifier().text,
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
                    name = it.Identifier().text,
                    init = null
                )
            },
            body = visit(ctx.block()) as ASTNode.Statement.Block
        )

    override fun visitClassDeclaration(ctx: MxLangParser.ClassDeclarationContext?) =
        ASTNode.Declaration.Class(
            location = Location(filename, ctx!!),
            name = ctx.Identifier().text,
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
                    name = it.Identifier().text,
                    init = it.expression()?.let { expression -> visit(expression) as ASTNode.Expression }
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
            else_ = null
        )

    override fun visitIfElseStatement(ctx: MxLangParser.IfElseStatementContext?) =
        ASTNode.Statement.If(
            location = Location(filename, ctx!!),
            condition = visit(ctx.expression()) as ASTNode.Expression,
            then = visit(ctx.thenStatement) as ASTNode.Statement,
            else_ = visit(ctx.elseStatement) as ASTNode.Statement
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
            initVariable = ctx.initVariableDeclaration
                ?.let { (visit(it) as ASTNode.Declaration.VariableList).list } ?: listOf(),
            initExpression = ctx.initExpression?.let { visit(it) as ASTNode.Expression },
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
            dimension = ctx.brack().size,
            length = ctx.brack().also {
                it[0].expression() ?: ASTErrorRecorder.error(
                    Location(filename, ctx), "length of first dimension must be specified"
                )
            }.map { it.expression()?.run { visit(this) as ASTNode.Expression } }
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
            child = ctx.Identifier().text
        )

    override fun visitMemberFunctionCall(ctx: MxLangParser.MemberFunctionCallContext?) =
        ASTNode.Expression.MemberFunction(
            location = Location(filename, ctx!!),
            base = visit(ctx.expression()) as ASTNode.Expression,
            name = ctx.Identifier().text,
            parameters = (visit(ctx.expressionList()) as ASTNode.Expression.ExpressionList).list
        )

    override fun visitFunctionCall(ctx: MxLangParser.FunctionCallContext?) =
        ASTNode.Expression.Function(
            location = Location(filename, ctx!!),
            name = ctx.Identifier().text,
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
                "++" -> SuffixOperator.INC
                "--" -> SuffixOperator.DEC
                else -> throw Exception("unknown suffix unary operator when building AST")
            }
        )

    override fun visitPrefixUnaryOperator(ctx: MxLangParser.PrefixUnaryOperatorContext?) =
        ASTNode.Expression.Prefix(
            location = Location(filename, ctx!!),
            operand = visit(ctx.expression()) as ASTNode.Expression,
            operator = when (ctx.op.text) {
                "++" -> PrefixOperator.INC
                "--" -> PrefixOperator.DEC
                "+" -> PrefixOperator.POS
                "-" -> PrefixOperator.NEG
                "!" -> PrefixOperator.L_NEG
                "~" -> PrefixOperator.INV
                else -> throw Exception("unknown prefix unary operator when building AST")
            }
        )

    override fun visitBinaryOperator(ctx: MxLangParser.BinaryOperatorContext?) =
        ASTNode.Expression.Binary(
            location = Location(filename, ctx!!),
            lhs = visit(ctx.expression(0)) as ASTNode.Expression,
            rhs = visit(ctx.expression(1)) as ASTNode.Expression,
            operator = when (ctx.op.text) {
                "*" -> BinaryOperator.TIMES
                "/" -> BinaryOperator.DIV
                "%" -> BinaryOperator.REM
                "+" -> BinaryOperator.PLUS
                "-" -> BinaryOperator.MINUS
                "<<" -> BinaryOperator.SHL
                ">>" -> BinaryOperator.SHR
                ">>>" -> BinaryOperator.U_SHR
                "<" -> BinaryOperator.LESS
                ">" -> BinaryOperator.GREATER
                "<=" -> BinaryOperator.LEQ
                ">=" -> BinaryOperator.GEQ
                "==" -> BinaryOperator.EQUAL
                "!=" -> BinaryOperator.UNEQUAL
                "&" -> BinaryOperator.A_AND
                "^" -> BinaryOperator.A_XOR
                "|" -> BinaryOperator.A_OR
                "&&" -> BinaryOperator.L_AND
                "||" -> BinaryOperator.L_OR
                "=" -> BinaryOperator.ASSIGN
                "+=" -> BinaryOperator.PLUS_I
                "-=" -> BinaryOperator.MINUS_I
                "*=" -> BinaryOperator.TIMES_I
                "/=" -> BinaryOperator.DIV_I
                "%=" -> BinaryOperator.REM_I
                "&=" -> BinaryOperator.AND_I
                "^=" -> BinaryOperator.XOR_I
                "|=" -> BinaryOperator.OR_I
                "<<=" -> BinaryOperator.SHL_I
                ">>=" -> BinaryOperator.SHR_I
                ">>>=" -> BinaryOperator.U_SHR_I
                else -> throw Exception("unknown binary operator when building AST")
            }
        )

    override fun visitTernaryOperator(ctx: MxLangParser.TernaryOperatorContext?) =
        ASTNode.Expression.Ternary(
            location = Location(filename, ctx!!),
            condition = visit(ctx.expression(0)) as ASTNode.Expression,
            then = visit(ctx.expression(1)) as ASTNode.Expression,
            else_ = visit(ctx.expression(2)) as ASTNode.Expression
        )

    override fun visitThisExpression(ctx: MxLangParser.ThisExpressionContext?) =
        ASTNode.Expression.This(
            location = Location(filename, ctx!!)
        )

    override fun visitIdentifiers(ctx: MxLangParser.IdentifiersContext?) =
        ASTNode.Expression.Identifier(
            location = Location(filename, ctx!!),
            name = ctx.Identifier().text
        )

    override fun visitConstants(ctx: MxLangParser.ConstantsContext?) =
        visit(ctx!!.constant()) as ASTNode.Expression

    override fun visitConstant(ctx: MxLangParser.ConstantContext?) =
        when {
            ctx!!.IntConstant() != null -> ASTNode.Expression.Constant.Int(
                location = Location(filename, ctx),
                value = ctx.IntConstant().text.toInt()
            )
            ctx.StringConstant() != null -> ASTNode.Expression.Constant.String(
                location = Location(filename, ctx),
                value = ctx.StringConstant().text
                    .removeSurrounding("\"")
                    .replace("\\t", "\t")
                    .replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\\\", "\\")
                    .replace("\\\"", "\"")
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
            dimension = ctx.brack().size
        )
}
