package personal.wuqing.rogue.ir.translator

import personal.wuqing.rogue.ast.ASTNode
import personal.wuqing.rogue.grammar.MxFunction
import personal.wuqing.rogue.grammar.MxVariable
import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.grammar.IRType
import personal.wuqing.rogue.ir.map.FunctionMap
import personal.wuqing.rogue.ir.map.GlobalMap
import personal.wuqing.rogue.ir.map.LiteralMap
import personal.wuqing.rogue.ir.map.TypeMap
import java.util.LinkedList

object TopLevelTranslator {
    private val blocks = mutableListOf<IRBlock>()
    val block get() = blocks.last()
    fun enterNewBlock(block: IRBlock) {
        if (!terminating) error("previous block not terminated")
        blocks += block
        terminating = false
    }

    private val phi get() = block.phi
    fun statement(statement: IRStatement.Phi) {
        if (normal.isNotEmpty()) error("non-phi statements before phi")
        if (!terminating) phi += statement
    }

    private val normal get() = block.normal
    fun statement(statement: IRStatement.Normal) {
        if (!terminating) normal += statement
    }

    fun statement(statement: IRStatement.Terminate) {
        if (!terminating) block.terminate = statement
        terminating = true
    }

    fun next(type: IRType) = IRItem.Local(type)
    val local = mutableMapOf<MxVariable, IRItem>()
    val loopTarget = mutableMapOf<ASTNode.Statement.Loop, Pair<IRBlock, IRBlock>>()
    var terminating = true
    var returnType: IRType? = null
    var thi: IRItem? = null

    val toProcess = LinkedList<IRFunction.Declared>()

    operator fun invoke(function: IRFunction.Declared): MutableList<IRBlock> {
        if (function.member) thi = function.namedArgs.first()
        blocks.clear()
        returnType = function.ret
        val entry = IRBlock(".entry")
        if (function.name == "main") {
            enterNewBlock(IRBlock(".init_global"))
            for ((variable, global) in GlobalMap.entries()) if (variable.declaration.init != null) {
                val value = ExpressionTranslator(variable.declaration.init).value.nullable(global.type)
                this.statement(IRStatement.Normal.Store(src = value, dest = global))
            }
            this.statement(IRStatement.Terminate.Jump(entry))
        }
        enterNewBlock(entry)
        function.ast.parameterList.zip(function.namedArgs.run { if (function.member) subList(1, size) else this })
            .forEach { (variable, arg) ->
                next(IRType.Address(arg.type)).also {
                    this.statement(IRStatement.Normal.Alloca(it))
                    this.statement(IRStatement.Normal.Store(src = arg, dest = it))
                    local[variable.actual] = it
                }
            }
        for (statement in function.ast.body.statements) StatementTranslator(statement)
        if (!terminating) this.statement(
            IRStatement.Terminate.Ret(
                when (function.ret) {
                    IRType.I32 -> IRType.I32 const 0
                    IRType.I1 -> IRType.I1 const 0
                    is IRType.Class -> IRItem.Null(function.ret)
                    IRType.Null -> error("returning null")
                    IRType.String -> LiteralMap[""]
                    is IRType.Array -> IRItem.Null(function.ret)
                    is IRType.Address -> error("returning address")
                    IRType.Void -> null
                }
            )
        )
        return blocks.toMutableList()
    }

    operator fun invoke(ast: ASTNode.Program, main: MxFunction): IRProgram {
        ast.declarations.filterIsInstance<ASTNode.Declaration.Variable>().map { it.actual }.forEach {
            GlobalMap[it] = IRItem.Global(TypeMap[it.type])
        }
        FunctionMap[main]
        while (toProcess.isNotEmpty()) toProcess.poll().body
        return IRProgram(
            struct = TypeMap.all(),
            literal = LiteralMap.all().toMutableSet(),
            global = GlobalMap.all().toMutableSet(),
            function = FunctionMap.all().toMutableSet()
        )
    }
}