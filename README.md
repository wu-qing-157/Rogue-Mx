# Mx-Compiler

The project for Compiler Design and Implementation at SJTU

## Progress

Description|Status
---|---
ANTLR4|Completed
AST|Completed
Semantic|Completed, __pass__ given test suite, __with__ known issues
IR Translation|No major progress
...|Not planned yet
Commandline Option|Currently with _commons-cli_, to be rewritten or removed

## Known Issues

+ Semantic: initialize primitive type with null

## Test Cases

Test Suite|Status
---|---
Semantic|__Test OK__
Codegen|Not planned yet

#### Custom Test Cases

File Name|Test Description|Status
---|---|---
000|CE Lexer|
001|CE Parser|
100|Hello World!|
101|Notation Test|
102|Associativity Test|
103|Non-ASCII Character Test|
104|Complicated Test|
200|Semantic Test|

Test cases starting with _9_ are not shared through _git_

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
