package personal.wuqing.mxcompiler.parser

import org.antlr.v4.gui.Trees
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

const val fileName = "test/102.mx"

fun main() {
    println("Lexer Test:")
    val lexer = MxLangLexer(CharStreams.fromFileName(fileName))
    println(lexer.allTokens.joinToString(" ") { it.text })
    println("Parser Test:")
    val parser = MxLangParser(CommonTokenStream(MxLangLexer(CharStreams.fromFileName(fileName))))
    val tree = parser.program()
    Trees.inspect(tree, parser)
    println(tree.toStringTree(parser))
}
