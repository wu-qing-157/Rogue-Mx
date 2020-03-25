package personal.wuqing.rogue.llvm

import personal.wuqing.rogue.llvm.grammar.IRBlock
import personal.wuqing.rogue.llvm.grammar.IRFunction
import personal.wuqing.rogue.llvm.grammar.IRGlobal
import personal.wuqing.rogue.llvm.grammar.IRProgram
import personal.wuqing.rogue.llvm.grammar.IRType

object IRPrinter {
    private val result = StringBuilder()

    private operator fun plusAssign(s: String) {
        result.append(s).append('\n')
    }

    operator fun invoke(program: IRProgram): String {
        result.clear()
        program.external.forEach { this(it) }
        program.struct.forEach { this(it) }
        program.global.forEach { this(it) }
        program.function.forEach { this(it) }
        return result.toString()
    }

    private operator fun invoke(global: IRGlobal) {
        this += global.toString()
    }

    private operator fun invoke(struct: IRType.Class) {
        this += struct.definition()
    }

    private operator fun invoke(function: IRFunction.Declared) {
        this += function.declaration + "{"
        function.body.forEach { this(it) }
        this += "}"
    }

    private operator fun invoke(external: IRFunction.External) {
        this += external.declaration
    }

    private operator fun invoke(block: IRBlock) {
        this += "  ${block.name}:"
        block.statements.forEach { this += "    $it" }
    }
}
