package personal.wuqing.rogue.parser

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ConsoleErrorListener
import personal.wuqing.rogue.utils.ParserErrorRecorder
import java.io.IOException
import java.io.InputStream

object ParserMain {
    operator fun invoke(input: InputStream, source: String) = try {
        val lexer = MxLangLexer(CharStreams.fromStream(input)).apply {
            removeErrorListener(ConsoleErrorListener.INSTANCE)
            addErrorListener(LexerErrorListener(source))
        }
        MxLangParser(CommonTokenStream(lexer)).apply {
            removeErrorListener(ConsoleErrorListener.INSTANCE)
            addErrorListener(ParserErrorListener(source))
        }
    } catch (e: IOException) {
        ParserErrorRecorder.fatalException(e.toString())
    }
}
