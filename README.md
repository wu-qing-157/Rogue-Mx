# Rogue-Mx

_Rogue-Mx_ is a compiler from _Mx**_ to _RISC-V, 32-bit, integer Extended_,
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
IR|Completed, __pass__ given test suite using naive codegen
Codegen|Completed, __pass__ given test suite
Optimize|Completed, outperform O1, nearly O2
GC|I'm thinking peach, it will not be implemented unless I'm spare enough to plant some peaches

## Known Issues

+ internal: built-in function does not override `toString()`

## Test Cases

The `test-tool` contains some legacy test tools before the whole pipeline is built.

Refer to [project assignment](https://github.com/peterzheng98/Compiler-2020) for test cases.

## Optimize

This section keeps track of implemented optimizations

### SSA

`Mem2Reg` was originally implemented for LLVM IR,
and started from the workaround `alloca`-`load`-`store` in LLVM IR format.
Current IR form differs from LLVM IR only in the discard of type system,
so it also starts from `alloca`-`load`-`store` format.

SSA is performed for all and only for all local variables,
thus `load`-`store` of anything like global variables, class members, etc.
is not optimized in this step.

Also, note that `alloca` is not supported in `RVTranslator`,
and any other optimization is based on SSA form,
so this step __must__ be executed.

This implementation uses the algorithm from
_Chapter 19_ of _Modern Compiler Implementation in C_.

### Aggressive Dead Code Elimination

Current implementation never eliminates control statements.

### Function Inline

Force small function to be inline.

_Small_ means that the number of instruction the result function contains
is no more than a specified figure, roughly several thousand.

Non-recursive calls are processed by their instruction number
from smaller to bigger.
Self-recursive calls are then processed at most three times.
Recurse involving multiple functions are not optimized.

### Constant Propagation

Only performed on local variables.

Including operations on string literals.

Requiring DCE after this.

### Global Localization

Localize global variables so that other optimizations can have effect on them.

Requiring Mem2Reg again after this.

TODO: load with DomTree, store with liveness analysis

Not enabled, as this is not a commmon method in compilers, ex., do not work for multi-file situation

### Andersen Alias Analysis

I finally decide to implement this.

### Loop Invariant Code Motion

Only performed on `ICalc` and `Load`.

Actually, it seems to optimize `ICmp` and `Branch`, together with `Phi`,
but it seems non-trivial, so it is not currently implemented.

### Common Subexpression Elimination

Performed on `Phi`, `ICalc`, `Call` and `Load`.

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
+ 2020.02.26 Fix with the latest test cases
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
+ 2020.03.25 __LLVM IR (should) pass the assigned test suite__ o(\*￣▽￣\*)ブ
+ 2020.03.26 Fix unicode character and unicode escape (but what is the use?)
+ 2020.03.26 Fix behavior when exception met
+ 2020.03.28 Implement a dominator tree for future use
+ 2020.03.29 __Force SSA form for all local variable__
+ 2020.03.30 Use kotlin script for gradle build script (Groovy really sucks!)
+ 2020.03.31 Fix a (terrible) issue about escape character (so poisonous)
+ 2020.04.03 Fix escape character (again, more poisonous)
+ 2020.04.07 Fix escape character (again and again, even more poisonous)
+ 2020.04.11 Greatly change IR
+ 2020.04.20 Write a naive codegen for IR test
+ 2020.04.21 Fix naive codegen
+ 2020.05.06 Complete codegen with virtual registers (untested)
+ 2020.05.24 __Finish Register Allocation__ (passing simple tests)
+ 2020.05.24 __Codegen (should) pass the assigned test suite__ φ(゜▽゜*)♪
+ 2020.05.24 Using MutableSet in register allocation rather than MutableList
+ 2020.05.24 Fix an issue about immediate
+ 2020.05.24 __Add Dead Code Elimination__
+ 2020.05.25 Add final optimizations on assembly
+ 2020.05.25 __Add Function Inline__
+ 2020.05.25 __Add Constant Propagation__
+ 2020.05.26 __Add Global Localization__
+ 2020.05.26 Add some simple optimizations
+ 2020.05.26 __Add Loop Invariant Code Motion__
+ 2020.05.27 __Add Common Subexpression Elimination__
+ 2020.05.27 __Add Andersen Alias Analysis__
+ 2020.05.27 Using Andersen to optimize load in LICM
+ 2020.05.27 Change inline policy
+ 2020.05.27 Fix Global Localization
+ 2020.05.27 Move global use-def analysis into Function Call Analysis
+ 2020.05.27 Using Andersen to optimize load in CSE

## How to build the compiler

To build this compiler, just execute
```shell script
sh gradlew installDist
```
or, on windows,
```shell script
gradlew.bat installDist
``` 
The result will be installed in `build/install/Rogue-Mx`.

_JDK 11_ is used for development. _JDK >= 1.8_ should be okay for build.

There is also another version in submodule _submit_,
which can be built offline with local resources in the judge docker.
To build it offline, just execute:
```shell script
sh gradlew installDist --offline
```

Library of built-in functions currently can only be generated by executing
```shell script
sh gradlew generateBuiltin
```
with
[riscv-gnu-toolchain](https://github.com/riscv/riscv-gnu-toolchain)
configured with:

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

### Discard of LLVM IR

The type system of LLVM IR is somehow annoying,
so it is deprecated.
A new IR is designed, which is also in CFG + SSA form,
just because SSA has already been implemented
when the decision is made.

It's also tried very hard to keep the compatibility to
output IR in LLVM IR form,
but it's not easy to add a fake type system,
and it's not worth it.

### Info about implementation

This part contains notes for future development.

#### `grammar.Variable`

+ only one `Variable` constructed for each declaration,
so unnecessary to override `equals()` and `hashCode()`
+ do not currently keep init info, access it using `Variable.def.init`

#### `semantic.SemanticMain`

+ maybe to `return` the `main` function in
`operator fun invoke(ASTNode.Program)` in the future,
as it is needed by `TopLevelTranslator`

#### `ast.ASTNode` `ast.ASTType`

+ `ASTNode` cannot be separated due to the limitation of `sealed class` (updated 2 years later, in Kotlin 1.4, why Kotlin does not evolve faster)
+ `ASTType` is separated from `ASTNode`
to prevent an extremely large source file

#### `ir.translator.ExpressionTranslator`

+ an unnecessary `load` is added for every assignment,
no plan to fix it, as it will be fixed naturally by dead code elimination

#### `riscv.RVInstruction`

+ maybe to keep reference to `RVBlock` rather than only the name
in `J` and `Branch` in the future
