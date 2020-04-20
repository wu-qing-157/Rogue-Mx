package personal.wuqing.rogue.riscv

object RVPrinter {
    operator fun invoke(program: RVProgram): String {
        val builder = StringBuilder()
        builder.appendln("\t.section\t.data")
        for (global in program.global) {
            builder.appendln("\t.globl\t$global")
            builder.appendln("$global:")
            builder.appendln("\t.zero\t4")
        }
        builder.appendln("\t.section\t.rodata")
        for (literal in program.literal) {
            builder.appendln("$literal:")
            builder.appendln("\t.word\t${literal.length}")
            builder.appendln("\t.string\t${literal.asmForm}")
        }
        builder.appendln("\t.text")
        for (function in program.function) {
            builder.appendln("\t.globl\t$function")
            function.instructions.joinTo(builder, separator = "\n", postfix = "\n")
        }
        return builder.toString()
    }
}
