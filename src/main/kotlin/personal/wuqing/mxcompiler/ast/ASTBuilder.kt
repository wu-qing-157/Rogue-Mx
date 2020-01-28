package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.parser.MxLangBaseVisitor
import personal.wuqing.mxcompiler.parser.MxLangParser
import personal.wuqing.mxcompiler.MxLangParserException
import personal.wuqing.mxcompiler.astErrorInfo
import personal.wuqing.mxcompiler.utils.Location

class ASTBuilder(private val filename: String) : MxLangBaseVisitor<ASTNode>() {
    override fun visitProgram(ctx: MxLangParser.ProgramContext?) = ProgramNode(
        location = Location(filename, ctx!!),
        declarations = ctx.section().map {
            when (val child = visit(it)) {
                is FunctionDeclarationNode, is ClassDeclarationNode -> listOf(child)
                is VariableDeclarationListNode -> child.list
                else -> listOf()
            }
        }.flatten().map { it as DeclarationNode }
    )

    override fun visitSection(ctx: MxLangParser.SectionContext?) = when {
        ctx!!.functionDeclaration() != null -> visit(ctx.functionDeclaration())!!
        ctx.classDeclaration() != null -> visit(ctx.classDeclaration())!!
        ctx.variableDeclaration() != null -> visit(ctx.variableDeclaration())!!
        else -> {
            println(
                astErrorInfo(
                    Location(filename, ctx),
                    "Unknown Section"
                )
            )
            throw MxLangParserException()
        }
    }

    override fun visitFunctionDeclaration(ctx: MxLangParser.FunctionDeclarationContext?) = FunctionDeclarationNode(
        location = Location(filename, ctx!!),
        name = ctx.Identifier().text,
        returnType = visit(ctx.type()) as TypeNode,
        parameterList = ctx.parameter().map {
            ParameterNode(
                location = Location(filename, it),
                type = visit(it.type()) as TypeNode,
                name = it.Identifier().text
            )
        }
    )

    override fun visitClassDeclaration(ctx: MxLangParser.ClassDeclarationContext?) = ClassDeclarationNode(
        location = Location(filename, ctx!!),
        name = ctx.Identifier().text,
        variables = ctx.variableDeclaration().map { (visit(it) as VariableDeclarationListNode).list }.flatten()
    )

    override fun visitVariableDeclaration(ctx: MxLangParser.VariableDeclarationContext?) = VariableDeclarationListNode(
        location = Location(filename, ctx!!),
        list = ctx.variable().map {
            VariableDeclarationNode(
                location = Location(filename, it),
                type = visit(ctx.type()) as TypeNode,
                name = it.Identifier().text,
                init = it.expression()?.let { expression -> visit(expression) as ExpressionNode }
            )
        }
    )

    override fun visitBlockStatement(ctx: MxLangParser.BlockStatementContext?) = BlockNode(
        location = Location(filename, ctx!!),
        statements = ctx.block().statement().map { visit(it) as StatementNode }
    )

    override fun visitExpressionStatement(ctx: MxLangParser.ExpressionStatementContext?) = ExpressionStatementNode(
        location = Location(filename, ctx!!),
        expression = visit(ctx.expression()) as ExpressionNode
    )

    override fun visitVariableDeclarationStatement(ctx: MxLangParser.VariableDeclarationStatementContext?) =
        VariableDeclarationStatementNode(
            location = Location(filename, ctx!!),
            variables = (visit(ctx.variableDeclaration()) as VariableDeclarationListNode).list
        )

    override fun visitReturnStatement(ctx: MxLangParser.ReturnStatementContext?) = ReturnNode(
        location = Location(filename, ctx!!),
        expression = visit(ctx.expression()) as ExpressionNode
    )

    override fun visitBreakStatement(ctx: MxLangParser.BreakStatementContext?) = BreakNode(Location(filename, ctx!!))

    override fun visitContinueStatement(ctx: MxLangParser.ContinueStatementContext?) =
        ContinueNode(Location(filename, ctx!!))

    override fun visitIfStatement(ctx: MxLangParser.IfStatementContext?) = IfNode(
        location = Location(filename, ctx!!),
        condition = visit(ctx.expression()) as ExpressionNode,
        thenStatement = visit(ctx.statement()) as StatementNode,
        elseStatement = null
    )

    override fun visitIfElseStatement(ctx: MxLangParser.IfElseStatementContext?) = IfNode(
        location = Location(filename, ctx!!),
        condition = visit(ctx.expression()) as ExpressionNode,
        thenStatement = visit(ctx.thenStatement) as StatementNode,
        elseStatement = visit(ctx.elseStatement) as StatementNode
    )

    override fun visitWhileStatement(ctx: MxLangParser.WhileStatementContext?) = WhileNode(
        location = Location(filename, ctx!!),
        condition = visit(ctx.expression()) as ExpressionNode,
        statement = visit(ctx.statement()) as StatementNode
    )

    override fun visitForStatement(ctx: MxLangParser.ForStatementContext?) = ForNode(
        location = Location(filename, ctx!!),
        initVariableDeclaration = ctx.initVariableDeclaration
            ?.let { (visit(it) as VariableDeclarationListNode).list } ?: listOf(),
        initExpression = ctx.initExpression?.let { visit(it) as ExpressionNode },
        condition = visit(ctx.condition) as ExpressionNode,
        step = ctx.step?.let { visit(it) as ExpressionNode },
        statement = visit(ctx.statement()) as StatementNode
    )

    override fun visitParentheses(ctx: MxLangParser.ParenthesesContext?) = visit(ctx!!.expression())!!

    // TODO: visiting all other expressions
}
