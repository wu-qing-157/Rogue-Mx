# Mx-Compiler

The project for Compiler Design and Implementation at SJTU

## Progress

Description|Status
---|---
ANTLR4|Completed
AST|Completed
Semantic|Completed, __pass__ given test suite
LLVM IR|Preliminarily completed, __a lot todo__, __partly untested__
...|Not planned yet

## Known Issues

+ compilation error unfixed in source set _test_
+ llvm: global variable does not support init
+ llvm: class member does not support init
+ llvm: unicode characters in string literal

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
global|Simple global variable|Pending re-test
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
null|Simple null test|__Passed__
...|...|much todo (orz)
array|Simple array operations|Not planned yet
if|Simple if|Not planned yet
while|Simple while|Not planned yet
for|Simple for|Not planned yet
control|Simple mixture of control statements|Not planned yet
...|...|Not planned yet

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
