package personal.wuqing.mxcompiler.utils

object LogPrinter {
    var printStream = System.out!!
    fun println(s: String) = printStream.println(s)
    fun print(s: String) = printStream.print(s)
}
