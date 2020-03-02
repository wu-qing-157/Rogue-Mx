set -e

if [ "$2" = "all" ]; then
    p=$(pwd)
    judge=$p/assignment/local-judge
    config=$judge/config.yaml
    dataset=$judge/testcase
    simulator=$p/assignment/ravel/build/bin
    echo "buildlimit: 120" > $config
    echo "instlimit: -1" >> $config
    echo "memlimit: 512" >> $config
    echo "path:" >> $config
    echo "  compiler: $p" >> $config
    echo "  dataset: $dataset" >> $config
    echo "  simulator: $simulator" >> $config
    echo "  simulator-executable: $simulator/ravel" >> $config
    echo "  built-in: $p/temp/builtin.s" >> $config
    echo "stage: $1" >> $config
    echo "timelimit: 15" >> $config
    (cd assignment/local-judge && python judge.py)
elif [ "$1" = "semantic" ]; then
    zsh gradlew installDist
    cp assignment/local-judge/testcase/sema/"$2"-package/"$2"-"$3".mx temp/test.mx
    cat --number temp/test.mx
    echo
    time mxc --semantic temp/test.mx
elif [ "$1" = "llvm" ]; then
    zsh gradlew installDist
    mxc --llvm temp/"$2".mx
    llc temp/"$2".ll
    gcc -o temp/"$2" temp/"$2".s temp/builtin.s
    echo "build successful"
    temp/"$2"
fi
