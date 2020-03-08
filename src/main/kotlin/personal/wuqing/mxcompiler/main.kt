package personal.wuqing.mxcompiler

import personal.wuqing.mxcompiler.ast.ASTBuilder
import personal.wuqing.mxcompiler.ast.ASTMain
import personal.wuqing.mxcompiler.io.OutputMethod
import personal.wuqing.mxcompiler.llvm.LLVMPrinter
import personal.wuqing.mxcompiler.llvm.LLVMTranslator
import personal.wuqing.mxcompiler.option.OptionMain
import personal.wuqing.mxcompiler.option.Target
import personal.wuqing.mxcompiler.parser.ParserMain
import personal.wuqing.mxcompiler.semantic.SemanticMain
import personal.wuqing.mxcompiler.utils.ANSI
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.ErrorRecorderException
import personal.wuqing.mxcompiler.utils.ParserErrorRecorder
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder
import java.io.InputStream
import kotlin.system.exitProcess

const val PROJECT_NAME = "Mx-Compiler"
val USAGE = ANSI.bold("mxc <sourcefiles> [options]")
const val VERSION = "0.9"

fun main(arguments: Array<String>) {
    when (val result = OptionMain(arguments)) {
        is OptionMain.Result.Exit -> exitProcess(99)
        is OptionMain.Result.FromSource -> {
            val (input, output, source, target) = result
            fromSource(input, output, source, target)
        }
    }
}

fun fromSource(input: InputStream, output: OutputMethod, source: String, target: Target) {
    try {
        val parser = ParserMain(input, source)
        val root = ASTMain(parser, source)
        ParserErrorRecorder.report()
        SemanticMain(root)
        ASTErrorRecorder.report()
        SemanticErrorRecorder.report()

        if (target == Target.SEMANTIC) return Unit.also { SemanticMain.reportSuccess() }

        val llvm = LLVMTranslator(root, SemanticMain.getMain())

        if (target == Target.LLVM) {
            output(LLVMPrinter(llvm))
            return
        }

        TODO("after generating LLVM")
    } catch (ast: ASTBuilder.Exception) {
        try {
            ParserErrorRecorder.report()
            ast.printStackTrace()
            exitProcess(1)
        } catch (parse: ErrorRecorderException) {
            exitProcess(parse.exit)
        }
    } catch (e: ErrorRecorderException) {
        exitProcess(e.exit)
    }
}
