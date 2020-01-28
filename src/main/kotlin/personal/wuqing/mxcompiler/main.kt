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
import personal.wuqing.mxcompiler.parser.LexerErrorListener
import personal.wuqing.mxcompiler.parser.MxLangLexer
import personal.wuqing.mxcompiler.parser.MxLangParser
import personal.wuqing.mxcompiler.parser.ParserErrorListener
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

enum class Target(val ext: String) {
    ALL(""), LEXER(".tokens"), TREE(".tree"), IR(".ir")
}

fun main(args: Array<String>) {
    val options = Options()
    options.addOption("h", "help", false, "Display this information")
    options.addOption("v", "version", false, "Display version information")
    options.addOption("in", "input-name", false, "Read file name from stdin")
    options.addOption("o", "output", true, "Specifying the destination")
    val targetOption = OptionGroup()
    targetOption.addOption(Option("l", "lexer", false, "Tokenize Source File Only"))
    targetOption.addOption(Option("t", "tree", false, "Generate Parser Tree Only"))
    targetOption.addOption(Option("I", "IR", false, "Generate IR Result Only"))
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
                    commandLine.hasOption("tree") -> Target.TREE
                    else -> Target.ALL
                }
                val outputFileName =
                    if (commandLine.hasOption("output")) commandLine.getOptionValue("output")
                    else inputFileName.replace(Regex("\\..*?$"), "") + target.ext
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

        val parser = MxLangParser(CommonTokenStream(lexer))
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
        val parserListener = ParserErrorListener(inputFileName)
        parser.addErrorListener(parserListener)

        val builder = ASTBuilder(inputFileName)
        val tree = parser.program()
        parserListener.report()
        if (target == Target.TREE) {
            val result = tree.toStringTree(parser).toByteArray()
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
    } catch (e: MxLangParserException) {
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
