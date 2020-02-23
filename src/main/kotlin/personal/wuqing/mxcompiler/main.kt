package personal.wuqing.mxcompiler

import personal.wuqing.mxcompiler.ast.ASTMain
import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.io.OutputMethod
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
        is OptionMain.Result.Exit -> exitProcess(1)
        is OptionMain.Result.FromSource -> {
            val (input, output, source, target) = result
            fromSource(input, output, source, target)
        }
        is OptionMain.Result.FromAST -> {
            val (root, output, source, target) = result
            fromAST(root, output, source, target)
        }
    }
}

fun fromSource(input: InputStream, output: OutputMethod, source: String, target: Target) {
    try {
        val parser = ParserMain(input, source)
        val root = ASTMain(parser, source)
        SemanticMain(root)

        ParserErrorRecorder.report()
        ASTErrorRecorder.report()
        SemanticErrorRecorder.report()

        when (target) {
            Target.AST -> return Unit.also {
                output(root)
                ASTErrorRecorder.report()
                SemanticErrorRecorder.report()
            }
            Target.SEMANTIC -> return Unit.also { SemanticMain.reportSuccess() }
            else -> fromAST(root, output, source, target)
        }
    } catch (e: ErrorRecorderException) {
        exitProcess(1)
    }
}

fun fromAST(root: ASTNode.Program, output: OutputMethod, source: String, target: Target) {
    TODO("after AST $root $output $source $target")
}
