# Mx-Compiler

The project for Compiler Design and Implementation at SJTU

## Progress

Description|Status
---|---
ANTLR4|Completed
AST|Completed
Semantic|Completed, __pass__ given test suite
LLVM IR|Preliminarily completed, __much todo__, __almost everything untested__
...|Not planned yet

## Known Issues

+ compilation error unfixed in source set _test_

## Test Cases

#### Given Test Suite

Test Suite|Status
---|---
Semantic|__Test OK__
Codegen|Not planned yet

#### Custom Test Cases

Test Case|Description|Status
---|---|---
return|Simple Main|__Passed__
hello|Hello World!|much todo (orz)
...|...|Not planned yet

## Timeline

+ 2020.01.14 Add a lexer & parser full of bugs
+ 2020.01.16 Lexer & Parser pass incomplete tests
+ 2020.01.16 Support non-ascii characters
+ 2020.01.22 Add commandline args interface
+ 2020.01.25 Add ASTNode and parts of ASTBuilder
+ 2020.01.28 Add commandline interface for generating parse tree
+ 2020.01.28 Add primary ASTBuilder (not tested)
+ 2020.01.29 Test ASTBuilder
+ 2020.01.29 Support output AST into file
+ 2020.02.05 Reconstruct part of ASTNode
+ 2020.02.22 Reconstruct structure of ASTNode
+ 2020.02.23 Add type analysis
+ 2020.02.23 Pass semantic test (orz)
+ 2020.02.23 Optimize project structure
+ 2020.02.24 Fix semantic about length of new array
+ 2020.02.24 Move new array check from AST build to semantic
+ 2020.02.24 Optimize project structure
+ 2020.02.25 Use different exit codes for different stages
+ 2020.02.25 Trivial fix
+ 2020.02.26 Fix with latest test cases
+ 2020.03.01 Fix known issue
+ 2020.03.01 Optimize semantic part for IR translation
+ 2020.03.02 Complete preliminary part of LLVM IR
