#!/bin/zsh -e

sh gradlew generateBuiltin installDist

typeset cyan='\033'\[36m
typeset blue='\033'\[34m
typeset green='\033'\[32m
typeset default='\033'\[0m

typeset testtool=$(dirname $0)
typeset testtool=$(cd $testtool && pwd)
typeset root="$testtool/.."
typeset judge="$root/assignment/local-judge"
typeset config="$judge/config.yaml"
typeset dataset="$judge/testcase"
typeset simulator=ravel
typeset built="$root/build/resources/main/builtin.s"
typeset testcase="$testtool/test"

prepareLocalJudge() {
    echo "sh gradlew installDist" > build.sh
    typeset stage=$1
    if [ "$1" = "semantic" ]; then
        typeset run=semantic
        echo "build/install/Rogue-Mx/bin/mxc --stdin --semantic" > semantic.sh
    elif [ "$1" = "llvm" ]; then
        typeset run=codegen
        echo "mxc --llvm --stdin --stdout | llc --march=riscv32 --mattr=+m - -o /dev/stdout" > codegen.sh
    elif [ "$1" = "codegen" ]; then
        exit 1
    else
        exit 1
    fi
    echo "buildlimit: 120" > $config
    echo "instlimit: -1" >> $config
    echo "memlimit: 512" >> $config
    echo "path:" >> $config
    echo "  compiler: $root" >> $config
    echo "  dataset: $dataset" >> $config
    echo "  simulator: $testtool" >> $config
    echo "  simulator-executable: $simulator" >> $config
    echo "  built-in: $built" >> $config
    echo "stage: $run" >> $config
    echo "timelimit: 15" >> $config
}

if [ "$2" = "all" ]; then
    prepareLocalJudge $1
    (cd "$judge" && python judge.py)
elif [ "$1" = "semantic" ]; then
    cp "$dataset"/sema/"$2"-package/"$2"-"$3".mx "$testtool"/test.mx
    cat --number "$testtool"/test.mx
    echo
    time mxc --semantic "$testtool"/test.mx
elif [ "$1" = "llvm" ]; then
    cp "$built" "$testtool/builtin.s"
    diff "$dataset"/codegen/"$2".mx "$testcase.mx" > /dev/null || (rm -f "$testcase.in" && vim -o "$testcase.in" "$dataset/codegen/$2.mx")
    cp "$dataset"/codegen/"$2".mx "$testcase.mx"
    echo $blue"mxc --llvm --steps"$default
    typeset TIMEFMT="compile time: %*E"
    time (mxc --llvm "$testcase.mx" && echo -n $cyan) && echo -n $default
    echo $blue"llc --march=riscv32 --mattr=+m"$default
    llc --march=riscv32 --mattr=+m "$testcase.ll"
    echo $blue"simulation"$default
    cd "$testtool" && ravel --oj-mode
    vimdiff "$testcase.out" "$testcase.mx"
fi

rm -f build.sh semantic.sh codegen.sh optimize.sh
