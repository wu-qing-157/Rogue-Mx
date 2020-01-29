package personal.wuqing.mxcompiler.ast

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import personal.wuqing.mxcompiler.parser.MxLangLexer
import personal.wuqing.mxcompiler.parser.MxLangParser
import java.io.FileOutputStream

const val fileName = "test/104.mx"
const val resultFile = "test/104.test"

fun main() {
    val lexer = MxLangLexer(CharStreams.fromFileName(fileName))
    val parser = MxLangParser(CommonTokenStream(lexer))
    val builder = ASTBuilder(fileName)
    val root = builder.visit(parser.program())
    val result = ASTPrinter.summary(root).toByteArray()
    val output = FileOutputStream(resultFile)
    output.write(result)
    output.close()
}
