# Mx-Compiler

This is a compiler from Mx** to RISC-V, 32 bit, integer Extended,
the project for _Compiler Design and Implementation_ at SJTU.
It's implemented mainly in Kotlin-JVM,
with the exception that the lexer and parser part contains Java,
since _antlr 4_ is used.

The language reference for Mx** may be found in the
[project assignment](https://github.com/peterzheng98/Compiler-2020).

## Progress

Description|Status
---|---
ANTLR4|Completed
AST|Completed
Semantic|Completed, __pass__ given test suite
LLVM IR|Almost completed, __pass custom tests__, __pending given tests__
...|Not planned yet

## Known Issues

+ compilation error unfixed in source set _test_
+ llvm: unicode characters in string literal
+ llvm: `null` does not work
+ custom-test: fail if '\\' in output, use debug mode can avoid this issue
+ internal: built-in function does not override `toString()`

## Test Cases

### Semantic Test

#### Given Test Suite

run with `sh test.sh semantic all` or `sh test.sh <package> <number>`

Status|Notes
---|---
187 / 187|__All Passed__

### LLVM Test

##### Custom Test

run with `sh custom-test.sh all` or `sh test.sh llvm <case>`

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
array-1|Simple array operations|__Passed__
array-2|Simple 2-dimension array|__Passed__
array-3|Simple interleaved array|__Passed__
substring|Simple substring|__Passed__
...|...|(maybe no more)

#### Given Test Suite

run with `sh test.sh llvm all` or `sh test.sh llvm <name>`

Test Case|Status|Description
---|---|---
t64|should pass|python's subprocess pipe is too slow
t25|should pass|pass on x86_64, simulation result loses the last line
t31|should pass|pass on x86_64, simulation result loses the last line
t61|fail|issue about null
t1|should pass|answer seems incorrect in the first line
t55|should pass|python's subprocess pipe is too slow
t65|fail|issue about null
t27|should pass|pass on x86_64, simulation result destroys the last line
t67|should pass|pass on x86_64, simulation not support some escape characters
t12|should pass|answer seems incorrect in exit code
e10|should pass|pass on x86_64, simulation fails mysteriously

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
+ 2020.03.07 Optimize project structure
+ 2020.03.08 Pass array tests
+ 2020.03.08 Fix an internal exception caused by lexer failure
+ 2020.03.15 Fix substring
+ 2020.03.16 Change implementation of string literal
+ 2020.03.18 Test LLVM IR with given test suite

## How to build the compiler

To build this compiler, just execute
```shell script
sh gradlew installDist
```
or, on windows,
```shell script
gradlew.bat installDist
```
and the result will be installed in `build/install/Mx-Compiler`.

JDK 11 are used for development. JDK >= 1.8 should be okay for build.

There is also another version in submodule _submit_,
which can be built offline with local resources in the judge docker.
To build it offline, just execute
```shell script
sh gradlew installDist --offline
```

Library of built-in functions currently can only be generated by executing
```shell script
sh gradlew generateBuiltin
```
with
[riscv-gnu-toolchain](https://github.com/riscv/riscv-gnu-toolchain)
configured with

```shell script
./configure --prefix=/opt/riscv --with-arch=rv32ima --with-abi=ilp32
```

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
and _Intellij IDEA_ takes a lot of time analyzing it,
so it maybe separated to multiple files in the future

#### Built-in functions

+ several implementation of built-in functions may be simplified
