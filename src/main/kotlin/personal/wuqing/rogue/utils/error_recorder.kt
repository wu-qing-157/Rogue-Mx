package personal.wuqing.rogue.utils

import personal.wuqing.rogue.DEBUG

open class ErrorRecorderException(val exit: Int) : Exception()

object OptionErrorRecorder {
    fun fatalError(msg: String) = LogPrinter.println("${ErrorType.Fatal} $msg")
    fun unsupported(msg: String) = LogPrinter.println("${ErrorType.Unsupported} $msg")
    fun warning(msg: String) = LogPrinter.println("${ErrorType.Warning} $msg")
    fun info(msg: String, newline: Boolean = true) =
        if (newline) LogPrinter.println("${ErrorType.Info} $msg")
        else LogPrinter.print("${ErrorType.Info} $msg")
}

object ParserErrorRecorder {
    object Exception : ErrorRecorderException(3)

    private var error = false
    fun report() = if (error) throw Exception else Unit
    fun exception(location: Location, msg: String) = LogPrinter.println("$location ${ErrorType.Error} $msg").also {
        error = true
    }

    fun fatalException(msg: String): Nothing {
        LogPrinter.println("${ErrorType.Fatal} $msg")
        throw Exception
    }
}

object ASTErrorRecorder {
    object Exception : ErrorRecorderException(4)

    private var error = false
    fun report() = if (error) throw Exception else Unit

    // fun warning(location: Location, msg: String) = LogPrinter.println("$location ${ErrorType.Warning} $msg")
    fun error(location: Location, msg: String) = LogPrinter.println("$location ${ErrorType.Error} $msg").also {
        error = true
    }
}

object LogRecorder {
    operator fun invoke(log: String) {
        if (DEBUG) LogPrinter.println("${ErrorType.Info} $log")
    }
}

object SemanticErrorRecorder {
    object Exception : ErrorRecorderException(5)

    private var error = false
    fun report() = if (error) throw Exception else Unit
    fun info(msg: String) = LogPrinter.println("${ErrorType.Info} $msg")

    // fun warning(location: Location, msg: String) = LogPrinter.println("$location ${ErrorType.Warning} $msg")
    fun error(location: Location, msg: String) = LogPrinter.println("$location ${ErrorType.Error} $msg").also {
        error = true
    }
}

object InternalExceptionRecorder {
    const val exit = 1

    operator fun invoke(e: Throwable) =
        if (DEBUG) e.printStackTrace()
        else LogPrinter.println("${ErrorType.Fatal} compiler internal error, use --debug to see more information")
}
