package personal.wuqing.rogue.ir

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRItem
import personal.wuqing.rogue.ir.grammar.IRProgram
import personal.wuqing.rogue.ir.grammar.IRStatement
import personal.wuqing.rogue.ir.grammar.IRType

object LLVMPrinter {
    private val result = StringBuilder()

    private operator fun plusAssign(s: String) {
        result.append(s).append('\n')
    }

    fun print(program: IRProgram): String {
        result.clear()
        program.struct.forEach { print(it) }
        program.global.forEach { print(it) }
        program.function.forEach { print(it) }
        return result.toString()
    }

    private fun print(global: IRItem) {
        this += global.toString()
    }

    private fun print(struct: IRType.Class) {
        this += struct.definition()
    }

    private fun print(function: IRFunction.Declared) {
        this += function.declaration + "{"
        function.body.forEach { this.print(it) }
        this += "}"
    }

    private fun print(external: IRFunction.External) {
        this += external.declaration
    }

    private fun print(block: IRBlock) {
        this += "  ${block.name}:"
        block.phi.forEach { this += "    $it" }
        block.normal.filter { it !is IRStatement.Normal.Nop }.forEach { this += "    $it" }
        block.terminate.let { this += "    $it" }
    }
}
