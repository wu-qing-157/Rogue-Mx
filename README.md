# Rogue-Mx

_Rogue-Mx_ is a compiler from _Mx**_ to _RISC-V, 32 bit, integer Extended_,
the project for _Compiler Design and Implementation_ at SJTU.
It's implemented mainly in _Kotlin/JVM_,
with the exception that the lexer and parser part contains _Java_,
since _antlr 4_ is used.

The language reference for _Mx**_ may be found in the
[project assignment](https://github.com/peterzheng98/Compiler-2020).

## Progress

Description|Status
---|---
ANTLR4|Completed
AST|Completed
Semantic|Completed, __pass__ given test suite
LLVM IR|Almost completed, __pass custom tests__, __pending given tests__
Optimize|Much to do, refer to _Optimize_ section
...|Not planned yet

## Known Issues

+ internal: built-in function does not override `toString()`

## Test Cases

Some scripts are in `test-tool` to automate testing.
They should be executed with _Z Shell_ in the project root folder.

Single-case custom test may fail if `\ ` in output,
no plan to fix this issue as it is rarely met.
Using debug mode can avoid this issue.

### Semantic Test

#### Given Test Suite

run with `assigned.sh semantic all` or `assigned.sh semantic <package> <number>`

Status|Notes
---|---
187 / 187|__All Passed__

### LLVM Test

##### Custom Test

run with `custom.sh llvm all` or `custom.sh llvm <case> [debug]`

Status|Notes
---|---
all cases|__All Passed__

#### Given Test Suite

run with `zsh assigned.sh llvm all` or `zsh assigned.sh llvm <name>`

Test Case|Status|Description
---|---|---
t64|should pass|python's subprocess pipe is too slow
t21|should pass|wrong standard output
t2|should pass|wrong standard output
t55|should pass|python's subprocess pipe is too slow
t65|should pass|escape character unsupported by ravel
t4|should pass|wrong standard output
e1|should pass|wrong standard output
t12|should pass|wrong standard exit code

## Optimize

This section keeps track of implemented optimizations

### TODO

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
+ 2020.03.25 Reconstruct LLVM type and name system
+ 2020.03.25 __LLVM IR (should) pass assigned test suite__ o(\*￣▽￣\*)ブ
+ 2020.03.26 Fix unicode character and unicode escape (but what is the use?)
+ 2020.03.26 Fix behavior when exception met
+ 2020.03.28 Implement a dominator tree for future use
+ 2020.03.29 __Force SSA form for all local variable__
+ 2020.03.30 Use kotlin script for gradle build script (Groovy really sucks!)
+ 2020.03.31 Fix a (terrible) issue about escape character (so poisonous)
+ 2020.04.03 Fix escape character (again, more poisonous)

## How to build the compiler

To build this compiler, just execute
```shell script
sh gradlew installDist
```
or, on windows,
```shell script
gradlew.bat installDist
```
and the result will be installed in `build/install/Rogue-Mx`.

_JDK 11_ are used for development. _JDK >= 1.8_ should be okay for build.

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
1|Nothing to execute or internal error
2|IO failure
3|Parser does not accept the code
4|AST builder does not accept the code
5|Semantic does not accept the code

### Info about implementation

This part contains notes for future development.

#### `grammar.Variable`

+ only one `Variable` constructed for each declaration,
so unnecessary to override `equals()` and `hashCode()`
+ currently do not keep init info, access it using `Variable.def.init`

#### `semantic.SemanticMain`

+ maybe to `return` the `main` function in
`operator fun invoke(ASTNode.Program)` in the future,
as it is needed by llvm `Translator`

#### `ast.ASTNode` `ast.ASTType`

+ `ASTNode` cannot be separated due to the limitation of `sealed class`
+ `ASTType` is separated from `ASTNode`
to prevent an extremely large source file

#### `llvm.Translator`

+ currently initialize global variables before the `entry.entry`
in `main` function,
maybe to add an extra `main` function to initialize global variables
and call the actual `main` function
+ this file is somehow large,
but currently no plan to separate it

#### Built-in functions

+ several implementation of built-in functions may be simplified
