package personal.wuqing.mxcompiler.llvm

object LLVMPrinter {
    private val result = StringBuilder()

    private operator fun plusAssign(s: String) {
        result.append(s).append('\n')
    }

    operator fun invoke(program: LLVMProgram): String {
        result.clear()
        result.append(header).append('\n')
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

    private operator fun invoke(block: LLVMBlock) {
        this += "${block.name.name}:"
        block.statements.forEach { this += "  $it" }
    }

    private val header = """
        declare i8* @__malloc__(i32)
        declare i32 @__getInt__()
        declare i8* @__getString__()
        declare void @__print__(i8*)
        declare void @__println__(i8*)
        declare void @__printlnInt__(i32)
        declare i8* @__toString__(i32)
        declare i32 @__string__length__(i8*)
        declare i32 @__string__ord__(i32, i8*)
        declare i32 @__string__parseInt__(i8*)
        declare i8* @__string__substring__(i32, i32, i8*)
        declare i8* @__string__concatenate__(i8*, i8*)
        declare i8 @__string__equal__(i8*, i8*)
        declare i8 @__string__neq__(i8*, i8*)
        declare i8 @__string__less__(i8*, i8*)
        declare i8 @__string__leq__(i8*, i8*)
        declare i8 @__string__greater__(i8*, i8*)
        declare i8 @__string__geq__(i8*, i8*)
        declare i32 @__array__size__(i8*)
        declare void @__empty__()
    """.trimIndent()
}
