package personal.wuqing.mxcompiler.parser

import personal.wuqing.mxcompiler.utils.Error
import personal.wuqing.mxcompiler.utils.Location

fun lexerExceptionInfo(location: Location, msg: String) = "$location $Error $msg"

fun parserExceptionInfo(location: Location, msg: String) = "$location $Error $msg"

class MxLangLexerException : Exception()

class MxLangParserException : Exception()
