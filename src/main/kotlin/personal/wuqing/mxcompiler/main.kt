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
import personal.wuqing.mxcompiler.parser.LexerErrorListener
import personal.wuqing.mxcompiler.parser.MxLangLexer
import personal.wuqing.mxcompiler.parser.MxLangParser
import personal.wuqing.mxcompiler.parser.ParserErrorListener
import personal.wuqing.mxcompiler.utils.ANSI
import personal.wuqing.mxcompiler.utils.FatalError
import personal.wuqing.mxcompiler.utils.Info
import personal.wuqing.mxcompiler.utils.LogPrinter
import personal.wuqing.mxcompiler.utils.Unsupported
import personal.wuqing.mxcompiler.utils.Warning
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import kotlin.system.exitProcess

const val PROJECT_NAME = "Mx-Compiler"
val USAGE = ANSI.bold("mxc <sourcefiles> [options]")
const val VERSION = "0.9"

enum class Target(private val description: String, val ext: String) {
    ALL("full compilation", ""),
    LEXER("token", ".tokens"),
    TREE("parse tree", ".tree"),
    AST("AST", ".ast"),
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
        addOption(Option(null, "lexer", false, "Generate Tokenized Source Only"))
        addOption(Option(null, "tree", false, "Generate Parser Tree Only"))
        addOption(Option(null, "ir", false, "Generate IR Result Only"))
        addOption(Option(null, "ast", false, "Generate AST Only"))
    })
    addOptionGroup(OptionGroup().apply {
        addOption(Option(null, "from-ast", false, "Compile from Generated AST"))
    })
}

fun main(args: Array<String>) {
    try {
        val commandLine = DefaultParser().parse(options, args)
        if (commandLine.hasOption("stdout")) LogPrinter.printStream = System.err
        when {
            commandLine.hasOption("help") -> {
                HelpFormatter().printHelp(USAGE, options)
                exitProcess(1)
            }
            commandLine.hasOption("version") -> {
                LogPrinter.println("$PROJECT_NAME $VERSION")
                exitProcess(1)
            }
            else -> {
                val source = when {
                    commandLine.hasOption("stdin") -> {
                        if (commandLine.args.isNotEmpty())
                            LogPrinter.println("$Warning input file ignored: ${commandLine.args.joinToString()}")
                        "stdin"
                    }
                    commandLine.hasOption("input-name") -> {
                        if (commandLine.args.isNotEmpty())
                            LogPrinter.println("$Warning input file ignored: ${commandLine.args.joinToString()}")
                        LogPrinter.print("$Info please input file name: ")
                        readLine()!!
                    }
                    commandLine.args.isEmpty() -> {
                        LogPrinter.println("$FatalError no input file")
                        throw CompilationFailedException()
                    }
                    commandLine.args.size > 1 -> {
                        LogPrinter.println("$Unsupported multiple input files")
                        throw CompilationFailedException()
                    }
                    else -> commandLine.args[0]
                }
                val input = when {
                    commandLine.hasOption("stdin") -> System.`in`
                    else -> FileInputStream(source)
                }
                val target = when {
                    commandLine.hasOption("lexer") -> Target.LEXER
                    commandLine.hasOption("ir") -> Target.IR
                    commandLine.hasOption("tree") -> Target.TREE
                    commandLine.hasOption("ast") -> Target.AST
                    else -> Target.ALL
                }
                val output = when {
                    commandLine.hasOption("stdout") -> System.out
                    commandLine.hasOption("output") -> FileOutputStream(commandLine.getOptionValue("output"))
                    else -> FileOutputStream(source.replace(Regex("\\..*?$"), "") + target.ext)
                }
                when {
                    commandLine.hasOption("from-ast") -> when (target) {
                        Target.LEXER, Target.TREE, Target.AST -> {
                            LogPrinter.println("$FatalError Cannot process $target from AST")
                            throw CompilationFailedException()
                        }
                        else ->
                            fromAST(ObjectInputStream(input).use {
                                it.readObject() as ASTNode
                            }, output, source, target)
                    }
                    else -> fromSource(input, output, source, target)
                }
            }
        }
    } catch (e: ParseException) {
        LogPrinter.println("$FatalError ${e.message}")
        HelpFormatter().printHelp(USAGE, options)
        exitProcess(1)
    } catch (e: IOException) {
        LogPrinter.println("$FatalError ${e.message}")
        exitProcess(1)
    } catch (e: CompilationFailedException) {
        exitProcess(1)
    }
}

fun fromSource(input: InputStream, output: OutputStream, source: String, target: Target) {
    try {
        val lexer = MxLangLexer(CharStreams.fromStream(input))
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
        val lexerListener = LexerErrorListener(source)
        lexer.addErrorListener(lexerListener)
        if (target == Target.LEXER) {
            val result = lexer.allTokens.joinToString(" ") { it.text }.toByteArray()
            lexerListener.report()
            output(result, output)
            return
        }

        val parser = MxLangParser(CommonTokenStream(lexer))
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
        val parserListener = ParserErrorListener(source)
        parser.addErrorListener(parserListener)

        val tree = parser.program()
        lexerListener.report()
        parserListener.report()
        if (target == Target.TREE) {
            val result = tree.toStringTree(parser).toByteArray()
            output(result, output)
            return
        }

        val builder = ASTBuilder(source)
        val root = builder.visit(tree)

        if (target == Target.AST) {
            output(root, output)
            return
        }

        fromAST(root, output, source, target)
    } catch (e: IOException) {
        LogPrinter.println("$FatalError ${e.message}")
        throw CompilationFailedException()
    } catch (e: MxLangLexerException) {
        throw CompilationFailedException()
    } catch (e: MxLangParserException) {
        throw CompilationFailedException()
    } catch (e: OutputFailedException) {
        throw CompilationFailedException()
    }
}

fun fromAST(root: ASTNode, output: OutputStream, source: String, target: Target) {
    println("$Info TEST OK") // TODO: after AST
}

fun output(bytes: ByteArray, output: OutputStream) {
    try {
        output.use { it.write(bytes) }
    } catch (e: IOException) {
        LogPrinter.println("$FatalError unable to output: ${e.message}")
        throw OutputFailedException()
    }
}

fun output(target: Any, output: OutputStream) {
    try {
        ObjectOutputStream(output).use { it.writeObject(target) }
    } catch (e: IOException) {
        LogPrinter.println("$FatalError unable to output: ${e.message}")
        throw OutputFailedException()
    }
}

class CompilationFailedException : Exception()

class OutputFailedException : Exception()
