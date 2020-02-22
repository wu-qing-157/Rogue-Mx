package personal.wuqing.mxcompiler.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import personal.wuqing.mxcompiler.utils.LexerErrorRecorder
import personal.wuqing.mxcompiler.utils.Location

class LexerErrorListener(private val fileName: String) : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) = LexerErrorRecorder.exception(Location(fileName, line, charPositionInLine), msg ?: "unknown error")
}
