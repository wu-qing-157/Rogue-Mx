package personal.wuqing.mxcompiler

import personal.wuqing.mxcompiler.utils.Error
import personal.wuqing.mxcompiler.utils.Location
import personal.wuqing.mxcompiler.utils.Warning

fun lexerExceptionInfo(location: Location, msg: String) = "$location $Error $msg"

fun parserExceptionInfo(location: Location, msg: String) = "$location $Error $msg"

fun astErrorInfo(location: Location, msg: String) = "$location $Error $msg"

fun astWarningInfo(location: Location, msg: String) = "$location $Warning $msg"

class MxLangLexerException : Exception()

class MxLangParserException : Exception()

class ASTException: Exception()
