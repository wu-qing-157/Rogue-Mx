package personal.wuqing.mxcompiler.option

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import personal.wuqing.mxcompiler.PROJECT_NAME
import personal.wuqing.mxcompiler.USAGE
import personal.wuqing.mxcompiler.VERSION
import personal.wuqing.mxcompiler.ast.ASTNode
import personal.wuqing.mxcompiler.io.OutputMethod
import personal.wuqing.mxcompiler.utils.OptionErrorRecorder
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream

object OptionMain {
    sealed class Result {
        object Exit : Result()
        data class FromSource(
            val input: InputStream, val output: OutputMethod, val source: String, val target: Target
        ) : Result()

        data class FromAST(
            val root: ASTNode.Program, val output: OutputMethod, val source: String, val target: Target
        ) : Result()
    }

    operator fun invoke(arguments: Array<String>) = try {
        DefaultParser().parse(options, arguments).run {
            when {
                hasOption("help") -> {
                    HelpFormatter().printHelp(USAGE, OptionMain.options)
                    Result.Exit
                }
                hasOption("version") -> {
                    OptionErrorRecorder.info("$PROJECT_NAME $VERSION")
                    Result.Exit
                }
                else -> {
                    val source = when {
                        hasOption("stdin") -> {
                            if (args.isNotEmpty())
                                OptionErrorRecorder.warning("input file ignored: ${args.joinToString()}")
                            "stdin"
                        }
                        hasOption("input-name") -> {
                            if (args.isNotEmpty())
                                OptionErrorRecorder.warning("input file ignored: ${args.joinToString()}")
                            OptionErrorRecorder.info("please input file name: ")
                            readLine().apply {
                                if (isNullOrEmpty()) return Result.Exit.also {
                                    OptionErrorRecorder.fatalError("empty input")
                                }
                            }!!
                        }
                        else -> args.singleOrNull() ?: return Result.Exit.also {
                            if (args.isEmpty()) OptionErrorRecorder.fatalError("no input file")
                            else OptionErrorRecorder.unsupported("multiple input files")
                        }
                    }
                    val input =
                        if (hasOption("stdin")) System.`in`
                        else FileInputStream(source)
                    val target = when {
                        hasOption("ir") -> Target.IR
                        hasOption("ast") -> Target.AST
                        hasOption("semantic") -> Target.SEMANTIC
                        else -> Target.ALL
                    }
                    val output = when {
                        hasOption("stdout") || hasOption("semantic") -> OutputMethod.Stdout
                        hasOption("output") -> OutputMethod.File(getOptionValue("output"))
                        else -> OutputMethod.File(source.replace(Regex("\\..*?$"), "") + target.ext)
                    }
                    when {
                        hasOption("from-ast") ->
                            if (target == Target.AST)
                                Result.Exit.also { OptionErrorRecorder.fatalError("Cannot process $target from AST") }
                            else
                                Result.FromAST(ObjectInputStream(input).use {
                                    it.readObject() as ASTNode.Program
                                }, output, source, target)
                        else -> Result.FromSource(input, output, source, target)
                    }
                }
            }
        }
    } catch (e: ParseException) {
        HelpFormatter().printHelp(USAGE, options)
        Result.Exit
    } catch (e: IOException) {
        OptionErrorRecorder.fatalError(e.toString())
        Result.Exit
    }

    private val options = Options().apply {
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
}
