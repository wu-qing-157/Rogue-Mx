package personal.wuqing.mxcompiler.utils

class Location(private val fileName: String, private val line: Int, private val charPositionInLine: Int) {
    override fun toString() = "\u001B[37;1m$fileName:$line:$charPositionInLine:\u001B[0m"
}
