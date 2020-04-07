package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.grammar.IRType
import personal.wuqing.rogue.ir.map.TypeMap

object StatementTranslator {
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
                else IRStatement.Terminate.Ret(
                    ExpressionTranslator(ast.expression).value.nullable(returnType ?: error("unspecified return type"))
                )
            )
        }
    }

    private operator fun invoke(ast: ASTNode.Statement.Variable) {
        ast.variables.forEach { variable ->
            val type = TypeMap[variable.type.type]
            next(IRType.Address(type)).also {
                statement(IRStatement.Normal.Alloca(it))
                local[variable.actual] = it
                variable.init?.let { init ->
                    statement(
                        IRStatement.Normal.Store(
                            ExpressionTranslator(init).value.nullable(TypeMap[variable.type.type]), it
                        )
                    )
                } ?: run {
                    statement(
                        IRStatement.Normal.Store(
                            when (type) {
                                IRType.I32 -> IRType.I32 const 0
                                IRType.I1 -> IRType.I1 const 0
                                is IRType.Class -> IRItem.Null(type)
                                else -> error("variable pointing to illegal type")
                            }, it
                        )
                    )
                }
            }
        }
    }

    private var ifCount = 0

    private operator fun invoke(ast: ASTNode.Statement.If) {
        val id = ifCount++
        val then = IRBlock(".if.$id.then")
        val els = IRBlock(".if.$id.else")
        val end = IRBlock(".if.$id.end")
        val condition = ExpressionTranslator(ast.condition).value
        statement(IRStatement.Terminate.Branch(condition, then, if (ast.els == null) end else els))
        enterNewBlock(then)
        this(ast.then)
        statement(IRStatement.Terminate.Jump(end))
        if (ast.els != null) {
            enterNewBlock(els)
            this(ast.els)
            statement(IRStatement.Terminate.Jump(end))
        }
        enterNewBlock(end)
    }

    private var whileCount = 0

    private operator fun invoke(ast: ASTNode.Statement.Loop.While) {
        val id = whileCount++
        val cond = IRBlock(".$id.while.condition")
        val body = IRBlock(".$id.while.body")
        val end = IRBlock(".$id.while.end")
        loopTarget[ast] = cond to end
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(cond)
        val condition = ExpressionTranslator(ast.condition).value
        statement(IRStatement.Terminate.Branch(condition, body, end))
        enterNewBlock(body)
        this(ast.statement)
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(end)
    }

    private var forCount = 0

    private operator fun invoke(ast: ASTNode.Statement.Loop.For) {
        if (ast.init != null) this(ast.init)
        val id = forCount++
        val cond = IRBlock(".$id.for.cond")
        val body = IRBlock(".$id.for.body")
        val end = IRBlock(".$id.for.end")
        val step = IRBlock(".$id.for.step")
        loopTarget[ast] = step to end
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(cond)
        val condition = ExpressionTranslator(ast.condition).value
        statement(IRStatement.Terminate.Branch(condition, body, end))
        enterNewBlock(body)
        this(ast.statement)
        statement(IRStatement.Terminate.Jump(step))
        enterNewBlock(step)
        ast.step?.let { ExpressionTranslator(it) }
        statement(IRStatement.Terminate.Jump(cond))
        enterNewBlock(end)
    }
}
