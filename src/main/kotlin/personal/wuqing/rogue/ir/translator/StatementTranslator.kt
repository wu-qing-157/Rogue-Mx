package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRStatement

object StatementTranslator {
    private fun next() = IRItem.Local()

    operator fun invoke(ast: ASTNode.Statement) {
        if (terminating) return
        when (ast) {
            is ASTNode.Statement.Empty -> Unit
            is ASTNode.Statement.Block -> ast.statements.forEach { this(it) }
            is ASTNode.Statement.Expression -> ExpressionTranslator(ast.expression)
            is ASTNode.Statement.Variable -> this(ast)
            is ASTNode.Statement.If -> this(ast)
            is ASTNode.Statement.Loop.While -> this(ast)
            is ASTNode.Statement.Loop.For -> this(ast)
            is ASTNode.Statement.Continue -> statement(
                IRStatement.Terminate.Jump(
                    loopTarget[ast.loop]?.first ?: error("loop target is uninitialized unexpectedly")
                )
            )
            is ASTNode.Statement.Break -> statement(
                IRStatement.Terminate.Jump(
                    loopTarget[ast.loop]?.second ?: error("loop target is uninitialized unexpectedly")
                )
            )
            is ASTNode.Statement.Return -> statement(
                if (ast.expression == null) IRStatement.Terminate.Ret(null)
                else IRStatement.Terminate.Ret(ExpressionTranslator(ast.expression).value)
            )
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.Variable) {
        ast.variables.forEach { variable ->
            next().also {
                statement(IRStatement.Normal.Alloca(it))
                local[variable.actual] = it
                variable.init?.let { init ->
                    statement(IRStatement.Normal.Store(ExpressionTranslator(init).value, it))
                } ?: statement(IRStatement.Normal.Store(IRItem.Const(0), it))
            }
        }
    }

    private var ifCount = 0

    private operator fun invoke(ast: ASTNode.Statement.If) {
        val id = ifCount++
        val then = IRBlock("if.$id.then")
        val els = IRBlock("if.$id.else")
        val end = IRBlock("if.$id.end")
        val condition = ExpressionTranslator(ast.condition).value
        statement(IRStatement.Terminate.Branch(condition, then, if (ast.els == null) end else els))
        if (ast.els != null) {
            enterNewBlock(els)
            this(ast.els)
            statement(IRStatement.Terminate.Jump(end))
        }
        enterNewBlock(then)
        this(ast.then)
        statement(IRStatement.Terminate.Jump(end))
        enterNewBlock(end)
    }

    private var whileCount = 0

    private operator fun invoke(ast: ASTNode.Statement.Loop.While) {
        val id = whileCount++
        val cond = IRBlock("while.$id.condition")
        val body = IRBlock("while.$id.body")
        val end = IRBlock("while.$id.end")
        loopTarget[ast] = cond to end
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(body)
        this(ast.statement)
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(cond)
        val condition = ExpressionTranslator(ast.condition).value
        statement(IRStatement.Terminate.Branch(condition, body, end))
        enterNewBlock(end)
    }

    private var forCount = 0

    private operator fun invoke(ast: ASTNode.Statement.Loop.For) {
        if (ast.init != null) this(ast.init)
        val id = forCount++
        val cond = IRBlock("for.$id.cond")
        val body = IRBlock("for.$id.body")
        val end = IRBlock("for.$id.end")
        val step = IRBlock("for.$id.step")
        loopTarget[ast] = step to end
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(body)
        this(ast.statement)
        statement(IRStatement.Terminate.Jump(step))
        enterNewBlock(step)
        ast.step?.let { ExpressionTranslator(it) }
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(cond)
        val condition = ExpressionTranslator(ast.condition).value
        statement(IRStatement.Terminate.Branch(condition, body, end))
        enterNewBlock(end)
    }
}
