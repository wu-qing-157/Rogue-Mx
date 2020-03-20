set -e

if [ "$2" = "all" ]; then
    echo "sh gradlew installDist" > build.sh
    if [ "$1" = "semantic" ]; then
        run=semantic
        echo "build/install/Rogue-Mx/bin/mxc --stdin --semantic" > semantic.sh
    elif [ "$1" = "llvm" ]; then
        run=codegen
        sh gradlew generateBuiltin
        echo "# This shell script is only for llvm IR test" > codegen.sh
        echo "build/install/Rogue-Mx/bin/mxc --llvm --stdin --stdout | llc --march=riscv32 --mattr=+m - -o /dev/stdout" >> codegen.sh
    elif [ "$1" = "codegen" ]; then
        exit 1
    else
        exit 1
    fi
    p=$(pwd)
    judge=$p/assignment/local-judge
    config=$judge/config.yaml
    dataset=$judge/testcase
    testdir=$p/test
    simulator=/opt/ravel/bin/ravel
    echo "buildlimit: 120" > $config
    echo "instlimit: -1" >> $config
    echo "memlimit: 512" >> $config
    echo "path:" >> $config
    echo "  compiler: $p" >> $config
    echo "  dataset: $dataset" >> $config
    echo "  simulator: $testdir" >> $config
    echo "  simulator-executable: $simulator" >> $config
    echo "  built-in: $p/build/resources/main/builtin_functions.s" >> $config
    echo "stage: $run" >> $config
    echo "timelimit: 15" >> $config
    (cd assignment/local-judge && python judge.py)
elif [ "$1" = "semantic" ]; then
    sh gradlew installDist
    cp assignment/local-judge/testcase/sema/"$2"-package/"$2"-"$3".mx test/test.mx
    cat --number test/test.mx
    echo
    time mxc --semantic test/test.mx
elif [ "$1" = "llvm" ]; then
    sh gradlew generateBuiltin
    sh gradlew installDist
    cp assignment/local-judge/testcase/codegen/"$2".mx test/test.mx
    echo "\e[34mmxc -llvm\e[0m"
    time mxc --llvm test/test.mx
    echo "\e[34mllc --march=riscv32 --mattr=+m test/test.ll"
    llc --march=riscv32 --mattr=+m test/test.ll
    echo "\e[34minput\e[0m"
    cat > test/test.in
    echo "\e[34msimulation\e[0m"
    cd test && ravel --oj-mode
fi
