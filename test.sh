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
else
    zsh gradlew installDist || return 98
    cp assignment/local-judge/testcase/sema/"$2"-package/"$2"-"$3".mx temp/test.mx || return 98
    cat --number temp/test.mx || return 98
    echo
    time build/install/Mx-Compiler/bin/mxc --semantic temp/test.mx
fi
