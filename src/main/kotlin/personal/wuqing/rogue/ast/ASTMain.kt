package personal.wuqing.rogue.ast

import personal.wuqing.rogue.parser.MxLangParser

object ASTMain {
    operator fun invoke(parser: MxLangParser, source: String) =
        ASTBuilder(source).visit(parser.program()) as ASTNode.Program
}
