package personal.wuqing.mxcompiler.utils

object LexerErrorRecorder {
    private var error = false
    class Exception : kotlin.Exception()
    fun report() = if (error) throw Exception() else Unit
    fun exception(location: Location, msg: String) = LogPrinter.println("$location $Error $msg").also {
        error = true
    }
}

object ParserErrorRecorder {
    private var error = false
    fun report() = if (error) throw Exception() else Unit
    class Exception : kotlin.Exception()
    fun exception(location: Location, msg: String) = LogPrinter.println("$location $Error $msg").also {
        error = true
    }
}

object ASTErrorRecorder {
    private var error = false
    fun report() = if (error) throw Exception() else Unit
    class Exception : kotlin.Exception()
    fun warning(location: Location, msg: String) = LogPrinter.println("$location $Warning $msg")
    fun error(location: Location, msg: String) = LogPrinter.println("$location $Error $msg").also {
        error = true
    }
}

object SemanticErrorRecorder {
    private var error = false
    fun report() = if (error) throw Exception() else Unit
    class Exception : kotlin.Exception()
    fun warning(location: Location, msg: String) = LogPrinter.println("$location $Warning $msg")
    fun error(location: Location, msg: String) = LogPrinter.println("$location $Error $msg").also {
        error = true
    }
}
