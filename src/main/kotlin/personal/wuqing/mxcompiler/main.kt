package personal.wuqing.mxcompiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.ConsoleErrorListener
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import personal.wuqing.mxcompiler.parser.LexerErrorListener
import personal.wuqing.mxcompiler.parser.MxLangLexer
import personal.wuqing.mxcompiler.parser.MxLangLexerException
import personal.wuqing.mxcompiler.utils.FatalError
import personal.wuqing.mxcompiler.utils.Info
import personal.wuqing.mxcompiler.utils.Unsupported
import personal.wuqing.mxcompiler.utils.Warning
import java.io.FileOutputStream
import java.io.IOException
import kotlin.system.exitProcess

const val PROJECT_NAME = "Mx-Compiler"
const val USAGE = "\u001B[37;1mMx-Compiler <sourcefiles> [options]\u001B[0m"
const val VERSION = "0.9"

enum class Target {
    ALL {
        override fun ext() = ""
    },
    LEXER {
        override fun ext() = ".tokens"
    },
    IR {
        override fun ext() = ".ir"
    };

    abstract fun ext(): String
}

fun main(args: Array<String>) {
    val options = Options()
    val helpOption = Option("h", "help", false, "Display this information")
    options.addOption(helpOption)
    val versionOption = Option("v", "version", false, "Display version information")
    options.addOption(versionOption)
    val inputNameOption = Option("in", "input-name", false, "Read file name from stdin")
    options.addOption(inputNameOption)
    val outputOption = Option("o", "output", true, "Specifying the destination")
    outputOption.argName = "output file"
    options.addOption(outputOption)
    val targetOption = OptionGroup()
    val lexerOption = Option("l", "lexer", false, "Tokenize Source File Only")
    targetOption.addOption(lexerOption)
    val irOption = Option("I", "IR", false, "Generate IR Result Only")
    targetOption.addOption(irOption)
    options.addOptionGroup(targetOption)

    try {
        val commandLine = DefaultParser().parse(options, args)
        when {
            commandLine.hasOption("help") -> {
                HelpFormatter().printHelp(USAGE, options)
                exitProcess(1)
            }
            commandLine.hasOption("version") -> {
                println("$PROJECT_NAME $VERSION")
                exitProcess(1)
            }
            else -> {
                val inputFileName = when {
                    commandLine.hasOption("input-name") -> {
                        if (commandLine.args.isNotEmpty())
                            println("$Warning input file ignored: ${commandLine.args.joinToString()}")
                        print("$Info please input file name: ")
                        readLine()!!
                    }
                    commandLine.args.isEmpty() -> {
                        println("$FatalError no input file")
                        throw CompilationFailedException()
                    }
                    commandLine.args.size > 1 -> {
                        println("$Unsupported multiple input files")
                        throw CompilationFailedException()
                    }
                    else -> commandLine.args[0]
                }
                val target = when {
                    commandLine.hasOption("lexer") -> Target.LEXER
                    commandLine.hasOption("IR") -> Target.IR
                    else -> Target.ALL
                }
                val outputFileName =
                    if (commandLine.hasOption("output")) commandLine.getOptionValue("output")
                    else inputFileName.replace(Regex("\\..*?$"), "") + target.ext()
                compile(inputFileName, outputFileName, target)
            }
        }
    } catch (e: ParseException) {
        println("$FatalError ${e.message}")
        HelpFormatter().printHelp(USAGE, options)
        exitProcess(1)
    } catch (e: CompilationFailedException) {
        exitProcess(1)
    }
}

fun compile(inputFileName: String, outputFileName: String, target: Target) {
    try {
        val lexer = MxLangLexer(CharStreams.fromFileName(inputFileName))
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
        val lexerListener = LexerErrorListener(inputFileName)
        lexer.addErrorListener(lexerListener)
        if (target == Target.LEXER) {
            val result = lexer.allTokens.joinToString(" ") { it.text }.toByteArray()
            lexerListener.report()
            output(result, outputFileName)
            return
        }
        if (target == Target.IR) {
            println("$Unsupported generate IR file")
            throw CompilationFailedException()
        }
        println("$Unsupported full compilation")
        throw CompilationFailedException()
    } catch (e: IOException) {
        println("$FatalError unable to open file $inputFileName")
        throw CompilationFailedException()
    } catch (e: MxLangLexerException) {
        throw CompilationFailedException()
    } catch (e: OutputFailedException) {
        throw CompilationFailedException()
    }
}

fun output(bytes: ByteArray, outputFileName: String) {
    try {
        val outputStream = FileOutputStream(outputFileName)
        outputStream.write(bytes)
        outputStream.close()
    } catch (e: IOException) {
        println("$FatalError unable to output: ${e.message}")
        throw OutputFailedException()
    }
}

class CompilationFailedException : Exception()

class OutputFailedException : Exception()
