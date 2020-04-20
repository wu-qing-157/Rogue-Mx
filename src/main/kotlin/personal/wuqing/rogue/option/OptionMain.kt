package personal.wuqing.rogue.option

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import personal.wuqing.rogue.PROJECT_NAME
import personal.wuqing.rogue.USAGE
import personal.wuqing.rogue.VERSION
import personal.wuqing.rogue.io.OutputMethod
import personal.wuqing.rogue.utils.OptionErrorRecorder
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream

object OptionMain {
    sealed class Result {
        class Exit(val exit: Int) : Result()
        data class FromSource(
            val input: InputStream, val output: OutputMethod, val source: String, val target: Target, val debug: Boolean
        ) : Result()
    }

    operator fun invoke(arguments: Array<String>) = try {
        DefaultParser().parse(options, arguments).run {
            when {
                hasOption("help") -> {
                    HelpFormatter().printHelp(USAGE, OptionMain.options)
                    Result.Exit(0)
                }
                hasOption("version") -> {
                    OptionErrorRecorder.info("$PROJECT_NAME $VERSION")
                    Result.Exit(0)
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
                            OptionErrorRecorder.info("please input file name: ", false)
                            readLine().apply {
                                if (isNullOrEmpty()) return Result.Exit(1).also {
                                    OptionErrorRecorder.fatalError("empty input")
                                }
                            }!!
                        }
                        else -> args.singleOrNull() ?: return Result.Exit(1).also {
                            if (args.isEmpty()) OptionErrorRecorder.fatalError("no input file")
                            else OptionErrorRecorder.unsupported("multiple input files")
                        }
                    }
                    val input = (if (hasOption("stdin")) System.`in` else FileInputStream(source)).let { input ->
                        if (hasOption("debug")) {
                            FileOutputStream("debug/source.mx").use { it.write(input.readAllBytes()) }
                            FileInputStream("debug/source.mx")
                        } else input
                    }
                    val target = when {
                        hasOption("semantic") -> Target.SEMANTIC
                        else -> Target.ALL
                    }
                    val output = when {
                        hasOption("stdout") || hasOption("semantic") -> OutputMethod.Stdout
                        hasOption("output") -> OutputMethod.File(getOptionValue("output"))
                        else -> OutputMethod.File(source.replace(Regex("\\..*?$"), "") + "." + target.ext)
                    }
                    Result.FromSource(input, output, source, target, hasOption("debug"))
                }
            }
        }
    } catch (e: ParseException) {
        OptionErrorRecorder.fatalError(e.message ?: "unknown")
        HelpFormatter().printHelp(USAGE, options)
        Result.Exit(1)
    } catch (e: IOException) {
        OptionErrorRecorder.fatalError(e.toString())
        Result.Exit(2)
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
            addOption(Option(null, "semantic", false, "Run Semantic"))
        })
        addOption(Option(null, "debug", false, "Output much intermediate result for debugging"))
    }
}
