package personal.wuqing.mxcompiler.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import personal.wuqing.mxcompiler.utils.ParserErrorRecorder
import personal.wuqing.mxcompiler.utils.Location

class ParserErrorListener(private val filename: String) : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) = ParserErrorRecorder.exception(Location(filename, line, charPositionInLine),
        (offendingSymbol as? CommonToken)?.let { "unexpected token ${it.text}" } ?: "unknown error")
}
