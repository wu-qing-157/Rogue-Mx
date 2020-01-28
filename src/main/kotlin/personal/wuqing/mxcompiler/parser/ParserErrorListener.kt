package personal.wuqing.mxcompiler.parser

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import personal.wuqing.mxcompiler.MxLangParserException
import personal.wuqing.mxcompiler.parserExceptionInfo
import personal.wuqing.mxcompiler.utils.Location

class ParserErrorListener(private val filename: String) : BaseErrorListener() {
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
        println(
            parserExceptionInfo(
                Location(filename, line, charPositionInLine),
                (offendingSymbol as? CommonToken)?.let { "unexpected token ${it.text}" } ?: "unknown error"))
    }

    fun report() = if (fail) throw MxLangParserException() else Unit
}
