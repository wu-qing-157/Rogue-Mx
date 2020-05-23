package personal.wuqing.rogue.riscv.grammar

class RVProgram(
    val global: List<RVGlobal>, val literal: List<RVLiteral>, val function: List<RVFunction>
)
