package personal.wuqing.mxcompiler.utils

import org.antlr.v4.runtime.ParserRuleContext

class Location(private val filename: String, private val line: Int, private val charPositionInLine: Int) {
    override fun toString() = "\u001B[37;1m$filename:$line:$charPositionInLine:\u001B[0m"

    constructor(filename: String, context: ParserRuleContext) : this(
        filename, context.getStart().line, context.getStart().charPositionInLine
    )
}
