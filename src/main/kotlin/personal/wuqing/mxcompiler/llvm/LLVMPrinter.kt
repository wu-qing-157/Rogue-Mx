package personal.wuqing.mxcompiler.llvm

object LLVMPrinter {
    private val result = StringBuilder()

    private operator fun plusAssign(s: String) {
        result.append(s).append('\n')
    }

    operator fun invoke(program: LLVMProgram): String {
        result.clear()
        program.external.forEach { this(it) }
        program.struct.forEach { this(it) }
        program.global.forEach { this(it) }
        program.function.forEach { this(it) }
        return result.toString()
    }

    private operator fun invoke(global: LLVMGlobal) {
        this += global.toString()
    }

    private operator fun invoke(struct: LLVMType.Class) {
        this += struct.definition()
    }

    private operator fun invoke(function: LLVMFunction.Declared) {
        this += function.definition()
        function.body.forEach { this(it) }
        this += "}"
    }

    private operator fun invoke(external: LLVMFunction.External) {
        this += external.toString()
    }

    private operator fun invoke(block: LLVMBlock) {
        this += "  ${block.name.name}:"
        block.statements.forEach { this += "    $it" }
    }
}
