package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.ASTException
import personal.wuqing.mxcompiler.astErrorInfo
import personal.wuqing.mxcompiler.parser.MxLangBaseVisitor
import personal.wuqing.mxcompiler.parser.MxLangParser
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.LogPrinter

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
            LogPrinter.println(astErrorInfo(Location(filename, ctx), "Unknown Section"))
            throw ASTException()
        }
    }

    override fun visitBlock(ctx: MxLangParser.BlockContext?) = BlockNode(
        location = Location(filename, ctx!!),
        statements = ctx.statement().map { visit(it) as StatementNode }
    )

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
        },
        body = visit(ctx.block()) as BlockNode
    )

    override fun visitConstructorDeclaration(ctx: MxLangParser.ConstructorDeclarationContext?) =
        ConstructorDeclarationNode(
            location = Location(filename, ctx!!),
            type = visit(ctx.type()) as TypeNode,
            parameterList = ctx.parameter().map {
                ParameterNode(
                    location = Location(filename, it),
                    type = visit(it.type()) as TypeNode,
                    name = it.Identifier().text
                )
            },
            body = visit(ctx.block()) as BlockNode
        )

    override fun visitClassDeclaration(ctx: MxLangParser.ClassDeclarationContext?) = ClassDeclarationNode(
        location = Location(filename, ctx!!),
        name = ctx.Identifier().text,
        variables = ctx.variableDeclaration().map { (visit(it) as VariableDeclarationListNode).list }.flatten(),
        functions = ctx.functionDeclaration().map { visit(it) as FunctionDeclarationNode },
        constructors = ctx.constructorDeclaration().map { visit(it) as ConstructorDeclarationNode }
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

    override fun visitEmptyStatement(ctx: MxLangParser.EmptyStatementContext?) = EmptyStatementNode(
        location = Location(filename, ctx!!)
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
        expression = ctx.expression()?.let { visit(it) as ExpressionNode }
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

    override fun visitNewObject(ctx: MxLangParser.NewObjectContext?) = NewObjectNode(
        location = Location(filename, ctx!!),
        baseType = visit(ctx.simpleType()) as TypeNode,
        parameters = ctx.expressionList()?.let {
            (visit(it) as ExpressionListNode).list
        } ?: listOf()
    )

    override fun visitNewArray(ctx: MxLangParser.NewArrayContext?) = NewArrayNode(
        location = Location(filename, ctx!!),
        baseType = visit(ctx.simpleType()) as TypeNode,
        dimension = ctx.indexBrack().size + ctx.brack().size,
        length = ctx.indexBrack().map { visit(it.expression()) as ExpressionNode }
    )

    override fun visitExpressionList(ctx: MxLangParser.ExpressionListContext?) = ExpressionListNode(
        location = Location(filename, ctx!!),
        list = ctx.expression().map { visit(it) as ExpressionNode }
    )

    override fun visitMemberAccess(ctx: MxLangParser.MemberAccessContext?) = MemberAccessNode(
        location = Location(filename, ctx!!),
        parent = visit(ctx.expression()) as ExpressionNode,
        child = ctx.Identifier().text
    )

    override fun visitMemberFunctionCall(ctx: MxLangParser.MemberFunctionCallContext?) = MemberFunctionCallNode(
        location = Location(filename, ctx!!),
        parent = visit(ctx.expression()) as ExpressionNode,
        name = ctx.Identifier().text,
        parameters = (visit(ctx.expressionList()) as ExpressionListNode).list
    )

    override fun visitFunctionCall(ctx: MxLangParser.FunctionCallContext?) = FunctionCallNode(
        location = Location(filename, ctx!!),
        name = ctx.Identifier().text,
        parameters = (visit(ctx.expressionList()) as ExpressionListNode).list
    )

    override fun visitIndexAccess(ctx: MxLangParser.IndexAccessContext?) = IndexAccessNode(
        location = Location(filename, ctx!!),
        parent = visit(ctx.expression(0)) as ExpressionNode,
        child = visit(ctx.expression(1)) as ExpressionNode
    )

    override fun visitSuffixUnaryOperator(ctx: MxLangParser.SuffixUnaryOperatorContext?) = SuffixUnaryNode(
        location = Location(filename, ctx!!),
        operand = visit(ctx.expression()) as ExpressionNode,
        operator = when (ctx.op.text) {
            "++" -> SuffixUnaryNode.SuffixOperator.INC
            "--" -> SuffixUnaryNode.SuffixOperator.DEC
            else -> {
                LogPrinter.println(astErrorInfo(Location(filename, ctx), "unknown suffix unary operator"))
                throw ASTException()
            }
        }
    )

    override fun visitPrefixUnaryOperator(ctx: MxLangParser.PrefixUnaryOperatorContext?) = PrefixUnaryNode(
        location = Location(filename, ctx!!),
        operand = visit(ctx.expression()) as ExpressionNode,
        operator = when (ctx.op.text) {
            "++" -> PrefixUnaryNode.PrefixOperator.INC
            "--" -> PrefixUnaryNode.PrefixOperator.DEC
            "+" -> PrefixUnaryNode.PrefixOperator.POSITIVE
            "-" -> PrefixUnaryNode.PrefixOperator.NEGATIVE
            "!" -> PrefixUnaryNode.PrefixOperator.LOGIC_NEGATION
            "~" -> PrefixUnaryNode.PrefixOperator.ARITHMETIC_NEGATION
            else -> {
                LogPrinter.println(astErrorInfo(Location(filename, ctx), "unknown prefix unary operator"))
                throw ASTException()
            }
        }
    )

    override fun visitBinaryOperator(ctx: MxLangParser.BinaryOperatorContext?) = BinaryNode(
        location = Location(filename, ctx!!),
        lhs = visit(ctx.expression(0)) as ExpressionNode,
        rhs = visit(ctx.expression(1)) as ExpressionNode,
        operator = when (ctx.op.text) {
            "*" -> BinaryNode.BinaryOperator.TIMES
            "/" -> BinaryNode.BinaryOperator.DIVIDE
            "%" -> BinaryNode.BinaryOperator.REM
            "+" -> BinaryNode.BinaryOperator.PLUS
            "-" -> BinaryNode.BinaryOperator.MINUS
            "<<" -> BinaryNode.BinaryOperator.SHIFT_LEFT
            ">>" -> BinaryNode.BinaryOperator.ARITHMETIC_SHIFT_RIGHT
            ">>>" -> BinaryNode.BinaryOperator.LOGIC_SHIFT_RIGHT
            "<" -> BinaryNode.BinaryOperator.LESS
            ">" -> BinaryNode.BinaryOperator.GREATER
            "<=" -> BinaryNode.BinaryOperator.LESS_EQUAL
            ">=" -> BinaryNode.BinaryOperator.GREATER_EQUAL
            "==" -> BinaryNode.BinaryOperator.EQUAL
            "!=" -> BinaryNode.BinaryOperator.NOT_EQUAL
            "&" -> BinaryNode.BinaryOperator.ARITHMETIC_AND
            "^" -> BinaryNode.BinaryOperator.ARITHMETIC_XOR
            "|" -> BinaryNode.BinaryOperator.ARITHMETIC_OR
            "&&" -> BinaryNode.BinaryOperator.LOGIC_AND
            "||" -> BinaryNode.BinaryOperator.LOGIC_OR
            "=" -> BinaryNode.BinaryOperator.ASSIGN
            "+=" -> BinaryNode.BinaryOperator.PLUS_ASSIGN
            "-=" -> BinaryNode.BinaryOperator.MINUS_ASSIGN
            "*=" -> BinaryNode.BinaryOperator.TIMES_ASSIGN
            "/=" -> BinaryNode.BinaryOperator.DIVIDE_ASSIGN
            "%=" -> BinaryNode.BinaryOperator.REM_ASSIGN
            "&=" -> BinaryNode.BinaryOperator.AND_ASSIGN
            "^=" -> BinaryNode.BinaryOperator.XOR_ASSIGN
            "|=" -> BinaryNode.BinaryOperator.OR_ASSIGN
            "<<=" -> BinaryNode.BinaryOperator.SHIFT_LEFT_ASSIGN
            ">>=" -> BinaryNode.BinaryOperator.ARITHMETIC_SHIFT_RIGHT_ASSIGN
            ">>>=" -> BinaryNode.BinaryOperator.LOGIC_SHIFT_RIGHT_ASSIGN
            else -> {
                LogPrinter.println(astErrorInfo(Location(filename, ctx), "unknown binary operator"))
                throw ASTException()
            }
        }
    )

    override fun visitTernaryOperator(ctx: MxLangParser.TernaryOperatorContext?) = TernaryNode(
        location = Location(filename, ctx!!),
        condition = visit(ctx.expression(0)) as ExpressionNode,
        thenExpression = visit(ctx.expression(1)) as ExpressionNode,
        elseExpression = visit(ctx.expression(2)) as ExpressionNode
    )

    override fun visitThisExpression(ctx: MxLangParser.ThisExpressionContext?) = ThisExpressionNode(
        location = Location(filename, ctx!!)
    )

    override fun visitIdentifiers(ctx: MxLangParser.IdentifiersContext?) = IdentifierExpressionNode(
        location = Location(filename, ctx!!),
        name = ctx.Identifier().text
    )

    override fun visitConstants(ctx: MxLangParser.ConstantsContext?) = visit(ctx!!.constant()) as ExpressionNode

    override fun visitConstant(ctx: MxLangParser.ConstantContext?) = when {
        ctx!!.IntConstant() != null -> IntConstantNode(
            location = Location(filename, ctx),
            value = ctx.IntConstant().text.toInt()
        )
        ctx.StringConstant() != null -> StringConstantNode(
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
        ctx.Null() != null -> NullConstantNode(Location(filename, ctx))
        ctx.True() != null -> TrueConstantNode(Location(filename, ctx))
        ctx.False() != null -> FalseConstantNode(Location(filename, ctx))
        else -> {
            LogPrinter.println(astErrorInfo(Location(filename, ctx), "unknown constant expression"))
            throw ASTException()
        }
    }

    override fun visitType(ctx: MxLangParser.TypeContext?) = when {
        ctx!!.simpleType() != null -> visitSimpleType(ctx.simpleType())
        ctx.arrayType() != null -> visitArrayType(ctx.arrayType())
        else -> {
            LogPrinter.println(astErrorInfo(Location(filename, ctx), "unknown type"))
            throw ASTException()
        }
    }

    override fun visitSimpleType(ctx: MxLangParser.SimpleTypeContext?) = SimpleTypeNode(
        location = Location(filename, ctx!!),
        name = ctx.text
    )

    override fun visitArrayType(ctx: MxLangParser.ArrayTypeContext?) = ArrayTypeNode(
        location = Location(filename, ctx!!),
        name = ctx.simpleType().text,
        dimension = ctx.brack().size
    )
}
