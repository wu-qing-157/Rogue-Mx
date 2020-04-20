package personal.wuqing.rogue.ir

import personal.wuqing.rogue.ir.grammar.IRBlock
import personal.wuqing.rogue.ir.grammar.IRFunction
import personal.wuqing.rogue.ir.grammar.IRProgram

object IRPrinter {
    operator fun invoke(program: IRProgram): String {
        val result = StringBuilder()
        program.global.joinTo(result, "") { "global $it\n" }
        if (program.global.isNotEmpty()) result.appendln()
        program.literal.joinTo(result, "") { "$it = ${it.irDisplay}\n" }
        if (program.literal.isNotEmpty()) result.appendln()
        program.function.forEach { func(result, it) }
        return result.toString()
    }

    private fun func(result: StringBuilder, func: IRFunction.Declared) {
        result.appendln("fun $func(${func.args.joinToString()})")
        func.body.forEach { block(result, it) }
        result.appendln("end")
        result.appendln()
    }

    private fun block(result: StringBuilder, block: IRBlock) {
        result.appendln("  ${block.name}:")
        block.phi.joinTo(result, "") { "    $it\n" }
        block.normal.joinTo(result, "") { "    $it\n" }
        result.appendln("    ${block.terminate}")
    }
}
