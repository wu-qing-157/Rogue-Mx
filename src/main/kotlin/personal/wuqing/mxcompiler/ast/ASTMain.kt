package personal.wuqing.mxcompiler.ast

import personal.wuqing.mxcompiler.parser.MxLangParser

object ASTMain {
    operator fun invoke(parser: MxLangParser, source: String) =
        ASTBuilder(source).visit(parser.program()) as ASTNode.Program
}
