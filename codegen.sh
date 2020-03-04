# In order to test llvm ir, currently use llc
mxc --stdin --stdout --llvm | llc --march=riscv32 --mattr=+m - -o /dev/stdout
