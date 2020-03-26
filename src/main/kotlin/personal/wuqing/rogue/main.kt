package personal.wuqing.rogue

import personal.wuqing.rogue.ast.ASTBuilder
import personal.wuqing.rogue.ast.ASTMain
import personal.wuqing.rogue.io.OutputMethod
import personal.wuqing.rogue.llvm.IRPrinter
import personal.wuqing.rogue.llvm.IRTranslator
import personal.wuqing.rogue.option.OptionMain
import personal.wuqing.rogue.option.Target
import personal.wuqing.rogue.parser.ParserMain
import personal.wuqing.rogue.semantic.SemanticMain
import personal.wuqing.rogue.utils.ANSI
import personal.wuqing.rogue.utils.ASTErrorRecorder
import personal.wuqing.rogue.utils.ErrorRecorderException
import personal.wuqing.rogue.utils.InternalExceptionRecorder
import personal.wuqing.rogue.utils.LogRecorder
import personal.wuqing.rogue.utils.ParserErrorRecorder
import personal.wuqing.rogue.utils.SemanticErrorRecorder
import java.io.IOException
import java.io.InputStream
import kotlin.system.exitProcess

const val PROJECT_NAME = "Rogue-Mx"
val USAGE = ANSI.bold("mxc <sourcefiles> [options]")
const val VERSION = "0.9"
var A64 = false; private set
var INFO = false; private set

fun main(arguments: Array<String>) {
    when (val result = OptionMain(arguments)) {
        is OptionMain.Result.Exit -> exitProcess(result.exit)
        is OptionMain.Result.FromSource -> {
            val (input, output, source, target) = result.apply { A64 = a64; INFO = info }
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
        LogRecorder("semantic passed successful")

        if (target == Target.SEMANTIC) return

        val llvm = IRTranslator(root, SemanticMain.getMain())

        if (target == Target.LLVM) {
            output(IRPrinter(llvm))
            return
        }

        TODO("after generating LLVM")
    } catch (ast: ASTBuilder.Exception) {
        try {
            ParserErrorRecorder.report()
            ast.printStackTrace()
            exitProcess(3)
        } catch (parse: ErrorRecorderException) {
            exitProcess(parse.exit)
        }
    } catch (e: ErrorRecorderException) {
        exitProcess(e.exit)
    } catch (e: NotImplementedError) {
        InternalExceptionRecorder(e)
        exitProcess(InternalExceptionRecorder.exit)
    } catch (e: IllegalStateException) {
        InternalExceptionRecorder(e)
        exitProcess(InternalExceptionRecorder.exit)
    } catch (e: IOException) {
        exitProcess(2)
    }
}
