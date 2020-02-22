package personal.wuqing.mxcompiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ConsoleErrorListener
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import personal.wuqing.mxcompiler.ast.ASTBuilder
import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.frontend.Semantic
import personal.wuqing.mxcompiler.parser.LexerErrorListener
import personal.wuqing.mxcompiler.parser.MxLangLexer
import personal.wuqing.mxcompiler.parser.MxLangParser
import personal.wuqing.mxcompiler.parser.ParserErrorListener
import personal.wuqing.mxcompiler.utils.ANSI
import personal.wuqing.mxcompiler.utils.ASTErrorRecorder
import personal.wuqing.mxcompiler.utils.FatalError
import personal.wuqing.mxcompiler.utils.FileOutput
import personal.wuqing.mxcompiler.utils.Info
import personal.wuqing.mxcompiler.utils.LexerErrorRecorder
import personal.wuqing.mxcompiler.utils.LogPrinter
import personal.wuqing.mxcompiler.utils.OutputMethod
import personal.wuqing.mxcompiler.utils.ParserErrorRecorder
import personal.wuqing.mxcompiler.utils.SemanticErrorRecorder
import personal.wuqing.mxcompiler.utils.StdoutOutput
import personal.wuqing.mxcompiler.utils.Unsupported
import personal.wuqing.mxcompiler.utils.Warning
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import kotlin.system.exitProcess

const val PROJECT_NAME = "Mx-Compiler"
val USAGE = ANSI.bold("mxc <sourcefiles> [options]")
const val VERSION = "0.9"

enum class Target(private val description: String, val ext: String) {
    ALL("full compilation", ""),
    AST("AST", ".ast"),
    SEMANTIC("SEMANTIC", "?"),
    IR("IR", ".ir");

    override fun toString() = description
}

val options = Options().apply {
    addOption(Option("h", "help", false, "Display this information"))
    addOption(Option("v", "version", false, "Display version information"))
    addOptionGroup(OptionGroup().apply {
        addOption(Option(null, "input-name", false, "Read input filename from stdin"))
        addOption(Option(null, "stdin", false, "Read source code from stdin"))
    })
    addOptionGroup(OptionGroup().apply {
        addOption(Option("o", "output", true, "Specifying the output filename").apply {
            argName = "filename"
        })
        addOption(Option(null, "stdout", false, "Output the result to stdout"))
    })
    addOptionGroup(OptionGroup().apply {
        addOption(Option(null, "ir", false, "Generate IR Result Only"))
        addOption(Option(null, "ast", false, "Generate AST Only"))
        addOption(Option(null, "semantic", false, "Run Semantic"))
    })
    addOptionGroup(OptionGroup().apply {
        addOption(Option(null, "from-ast", false, "Compile from Generated AST"))
    })
}

fun main(arguments: Array<String>) {
    try {
        DefaultParser().parse(options, arguments).apply {
            when {
                hasOption("help") -> {
                    HelpFormatter().printHelp(USAGE, personal.wuqing.mxcompiler.options)
                    exitProcess(1)
                }
                hasOption("version") -> {
                    LogPrinter.println("$PROJECT_NAME $VERSION")
                    exitProcess(1)
                }
                else -> {
                    val source = when {
                        hasOption("stdin") -> {
                            if (args.isNotEmpty())
                                LogPrinter.println("$Warning input file ignored: ${args.joinToString()}")
                            "stdin"
                        }
                        hasOption("input-name") -> {
                            if (args.isNotEmpty())
                                LogPrinter.println("$Warning input file ignored: ${args.joinToString()}")
                            LogPrinter.print("$Info please input file name: ")
                            readLine()!!
                        }
                        else -> args.singleOrNull() ?: throw CompilationFailedException().also {
                            LogPrinter.println(
                                if (args.isEmpty()) "$FatalError no input file"
                                else "$Unsupported multiple input files"
                            )
                        }
                    }
                    val input = if (hasOption("stdin")) System.`in`
                    else FileInputStream(source)
                    val target = when {
                        hasOption("ir") -> Target.IR
                        hasOption("ast") -> Target.AST
                        hasOption("semantic") -> Target.SEMANTIC
                        else -> Target.ALL
                    }
                    val output = when {
                        hasOption("stdout") || hasOption("semantic") -> StdoutOutput
                        hasOption("output") -> FileOutput(getOptionValue("output"))
                        else -> FileOutput(source.replace(Regex("\\..*?$"), "") + target.ext)
                    }
                    when {
                        hasOption("from-ast") -> when (target) {
                            Target.AST -> {
                                LogPrinter.println("$FatalError Cannot process $target from AST")
                                throw CompilationFailedException()
                            }
                            else ->
                                fromAST(ObjectInputStream(input).use {
                                    it.readObject() as ASTNode.Program
                                }, output, source, target)
                        }
                        else -> fromSource(input, output, source, target)
                    }
                }
            }
        }
    } catch (e: ParseException) {
        LogPrinter.println("$FatalError $e")
        HelpFormatter().printHelp(USAGE, options)
        exitProcess(1)
    } catch (e: IOException) {
        LogPrinter.println("$FatalError $e")
        exitProcess(1)
    } catch (e: CompilationFailedException) {
        exitProcess(1)
    }
}

fun fromSource(input: InputStream, output: OutputMethod, source: String, target: Target) {
    try {
        val lexer = MxLangLexer(CharStreams.fromStream(input))
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
        val lexerListener = LexerErrorListener(source)
        lexer.addErrorListener(lexerListener)

        val parser = MxLangParser(CommonTokenStream(lexer))
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
        val parserListener = ParserErrorListener(source)
        parser.addErrorListener(parserListener)

        val root = ASTBuilder(source).visit(parser.program()) as ASTNode.Program

        Semantic.run(root)

        LexerErrorRecorder.report()
        ParserErrorRecorder.report()
        ASTErrorRecorder.report()
        SemanticErrorRecorder.report()

        when (target) {
            Target.AST -> return Unit.also { output.output(root) }
            Target.SEMANTIC -> return Unit.also { LogPrinter.println("$Info semantic passed successfully") }
            else -> fromAST(root, output, source, target)
        }
    } catch (e: IOException) {
        LogPrinter.println("$FatalError $e")
        throw CompilationFailedException()
    } catch (e: LexerErrorRecorder.Exception) {
        throw CompilationFailedException()
    } catch (e: ParserErrorRecorder.Exception) {
        throw CompilationFailedException()
    } catch (e: ASTErrorRecorder.Exception) {
        throw CompilationFailedException()
    } catch (e: SemanticErrorRecorder.Exception) {
        throw CompilationFailedException()
    }
}

fun fromAST(root: ASTNode.Program, output: OutputMethod, source: String, target: Target) {
    TODO("after AST $root $output $source $target")
}

class CompilationFailedException : Exception()
