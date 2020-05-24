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
    echo "mxc --stdin --stdout" > codegen.sh
    echo "buildlimit: 120" > $config
    echo "instlimit: -1" >> $config
    echo "memlimit: 512" >> $config
    echo "path:" >> $config
    echo "  compiler: $root" >> $config
    echo "  dataset: $dataset" >> $config
    echo "  simulator: $testtool" >> $config
    echo "  simulator-executable: $simulator" >> $config
    echo "  built-in: $built" >> $config
    echo "stage: codegen" >> $config
    echo "timelimit: 15" >> $config
}

prepareLocalJudge $1
(cd "$judge" && python judge.py)

rm -f build.sh semantic.sh codegen.sh optimize.sh
