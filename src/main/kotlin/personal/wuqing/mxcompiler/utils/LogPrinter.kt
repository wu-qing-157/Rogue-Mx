package personal.wuqing.mxcompiler.utils

internal object LogPrinter {
    private val printStream = System.err!!
    fun println(s: String) = printStream.println(s)
}
