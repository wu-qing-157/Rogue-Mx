package personal.wuqing.rogue.riscv

import personal.wuqing.rogue.riscv.grammar.RVProgram

object RVPrinter {
    operator fun invoke(program: RVProgram): String {
        val builder = StringBuilder()
        builder.appendln("\t.section\t.data")
        for (global in program.global) {
            builder.appendln("\t.globl\t$global")
            builder.appendln("$global:")
            builder.appendln("\t.zero\t4")
        }
        builder.appendln()
        builder.appendln("\t.section\t.rodata")
        for (literal in program.literal) {
            builder.appendln("$literal:")
            builder.appendln("\t.word\t${literal.length}")
            builder.appendln("\t.string\t${literal.asmForm}")
        }
        builder.appendln()
        builder.appendln("\t.text")
        for (function in program.function) {
            builder.appendln("\t.globl\t$function")
            for (block in function.body) {
                builder.appendln("${block.name}:")
                block.instructions.forEach { builder.appendln(it) }
            }
            builder.appendln()
        }
        return builder.toString()
    }
}
