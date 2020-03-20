package personal.wuqing.rogue.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import personal.wuqing.rogue.utils.Location
import personal.wuqing.rogue.utils.ParserErrorRecorder

class LexerErrorListener(private val fileName: String) : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) = ParserErrorRecorder.exception(Location(fileName, line, charPositionInLine), msg ?: "unknown error")
}
