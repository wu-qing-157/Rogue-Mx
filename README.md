# Mx-Compiler

The project for Compiler Design and Implementation at SJTU

## Progress

Description|Status
---|---
ANTLR4|Completed
AST|Completed
Semantic|Completed, __pass__ given test suite
LLVM IR|Almost completed, __partly untested__
...|Not planned yet

## Known Issues

+ compilation error unfixed in source set _test_
+ llvm: unicode characters in string literal
+ grammar: built-in function does not override `toString()`

## Test Cases

### Semantic Test

#### Given Test Suite

run with `sh test.sh semantic all` or `sh test.sh <package> <number>`

Status|Notes
---|---
187 / 187|__All Passed__

### LLVM Test

##### Custom Test

run with `sh test.sh llvm custom-all` or `sh test.sh llvm <case>`

Test Case|Description|Status
---|---|---
return|Simple main|__Passed__
plus|Simple operator|__Passed__
global|Simple global variable|__Passed__
suffix|Simple suffix operator|__Passed__
prefix|Simple prefix operator|__Passed__
assign|Simple assignment|__Passed__
printInt|Call built-in function|__Passed__
hello|Hello World!|__Passed__
function|Simple function and call|__Passed__
ternary|Simple ternary expression|__Passed__
string|Simple string operation|__Passed__
bool|Simple bool operation|__Passed__
class-1|Simple class definition|__Passed__
class-2|Simple constructor|__Passed__
class-3|Simple member function|__Passed__
class-4|Simple member init|__Passed__
null|Simple null test|__Passed__
if|Simple if|__Passed__
while|Simple while|__Passed__
for|Simple for|__Passed__
control|Simple mixture of control statements|__Passed__
array|Simple array operations|A lot todo (orz)
...|...|(maybe no more)

#### Given Test Suite

run with `sh test.sh llvm all`

Status|Notes
---|---
???|Not planned yet

## Timeline

+ 2020.01.14 __Add a lexer & parser full of bugs__
+ 2020.01.16 Lexer & Parser pass incomplete tests
+ 2020.01.16 Support non-ascii characters
+ 2020.01.22 Add commandline args interface
+ 2020.01.25 Add ASTNode and parts of ASTBuilder
+ 2020.01.28 Add commandline interface for generating parse tree
+ 2020.01.28 __Add primary ASTBuilder (not tested)__
+ 2020.01.29 __Test ASTBuilder__
+ 2020.01.29 Support output AST into file
+ 2020.02.05 Reconstruct part of ASTNode
+ 2020.02.22 Reconstruct structure of ASTNode
+ 2020.02.23 Add type analysis
+ 2020.02.23 __Pass semantic test (orz)__
+ 2020.02.23 Optimize project structure
+ 2020.02.24 Fix semantic about length of new array
+ 2020.02.24 Move new array check from AST build to semantic
+ 2020.02.24 Optimize project structure
+ 2020.02.25 Use different exit codes for different stages
+ 2020.02.25 Trivial fix
+ 2020.02.26 Fix with latest test cases
+ 2020.03.01 Fix known issue
+ 2020.03.01 Optimize semantic part for IR translation
+ 2020.03.02 __Complete preliminary part of LLVM IR__
+ 2020.03.03 String literal now allows unicode escape
+ 2020.03.04 Test LLVM IR (stage 1)
+ 2020.03.04 Discard outputting AST (sad)
+ 2020.03.04 __LLVM IR now produce only used top-level things__
+ 2020.03.04 Remove unnecessary modifier data of some classes
+ 2020.03.04 Adjust class-type in LLVM IR
+ 2020.03.05 Test class constructor
+ 2020.03.05 Support null value
+ 2020.03.05 Test member function, fix identifier resolve
+ 2020.03.05 Pass control test
+ 2020.03.06 Support init global variable and class member


## Some notes

### Exit code info

Exit code|description
---|---
1|Unexpected compiler internal exception
2|Parser does not accept the code
3|AST builder does not accept the code
4|Semantic does not accept the code
99|Option parser does not generate any executable work

### Info about implementation

This part contains notes for future development.

#### `option.OptionMain` `io.OutputMethod`

+ maybe to use separate exit code for `IOException` in the future

#### `grammar.Variable`

+ only one `Variable` constructed for each declaration,
so unnecessary to override `equals()` and `hashCode()`
+ currently do not keep init info, access it using `Variable.def.init`

#### `semantic.SemanticMain`

+ maybe to `return` the `main` function in
`operator fun invoke(ASTNode.Program)` in the future,
as it is needed by llvm `Translator`

#### `ast.ASTNode` `personal.wuqiing.ast.ASTType`

+ `ASTNode` cannot be separated due to the limitation of `sealed class`
+ `ASTType` is separated from `ASTNode`
to prevent an extremely large source file

#### `llvm.LLVMType`

+ an `LLVMType.Pointer` may be constructed multiple time,
if needed in the future, a `data` modifier will be added

#### `llvm.LLVMName`

+ currently every subclass is a `data class`
+ however, only `LLVMName.Const` and `LLVMName.Global("main")`
may be constructed multiple times for same value,
so the `data` modifier may be removed in the future

#### `llvm.LLVMStatement`

+ some members of the subclasses may be unnecessary,
so maybe to delete them in the future

#### `llvm.Translator`

+ currently initialize global variables before the `__entry__`
in `main` function,
maybe to add an extra `main` function to initialize global variables
and call the actual `main` function
+ currently use `kotlin.Exception` for all unexpected situation,
may be changed into a specified `Exception` in the future,
also, another alternative may be to `printStackTrace()` only when
`--debug` option is specified, as these `Exception` should not
appear in final version
+ this file is somehow large,
and _Intellij IDEA_ takes a lot time analyzing it,
so it maybe separated to multiple files in the future
