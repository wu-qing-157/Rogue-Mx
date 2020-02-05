package personal.wuqing.mxcompiler.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import personal.wuqing.mxcompiler.MxLangLexerException
import personal.wuqing.mxcompiler.lexerExceptionInfo
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.LogPrinter

class LexerErrorListener(private val fileName: String) : BaseErrorListener() {
    private var fail = false

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        fail = true
        LogPrinter.println(lexerExceptionInfo(Location(fileName, line, charPositionInLine), msg ?: "unknown error"))
    }

    fun report() = if (fail) throw MxLangLexerException() else Unit
}
