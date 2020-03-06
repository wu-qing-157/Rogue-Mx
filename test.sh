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
    echo "  built-in: $p/build/resources/main/builtin_functions.s" >> $config
    echo "stage: $1" >> $config
    echo "timelimit: 15" >> $config
    (cd assignment/local-judge && python judge.py)
elif [ "$1" = "semantic" ]; then
    zsh gradlew installDist
    cp assignment/local-judge/testcase/sema/"$2"-package/"$2"-"$3".mx test/test.mx
    cat --number test/test.mx
    echo
    time mxc --semantic test/test.mx
elif [ "$1" = "llvm" ]; then
    echo "\e[34mgenerate built-in functions\e[0m"
    gcc -S src/main/resources/builtin_functions.c -o test/llvm/builtin_functions.s
    if [ "$2" = "custom-all" ]; then
        echo "\e[34mbuild compiler\e[0m"
        zsh gradlew installDist
        for name in $(cat test/llvm/list.txt); do
            echo "\e[34mtestcase $name:\e[0m"
            \time -f "    compile time: %E" mxc --llvm test/llvm/"$name".mx
            llc test/llvm/"$name".ll
            gcc -o test/llvm/"$name" test/llvm/"$name".s test/llvm/builtin_functions.s -no-pie
            echo "    \e[32mbuild successful\e[0m"
            if test -f test/llvm/"$name".in; then
                test/llvm/"$name" < test/llvm/"$name".in > test/llvm/"$name".out || echo exit code: $? >> test/llvm/"$name".out
            else
                test/llvm/"$name" > test/llvm/"$name".out || echo exit code: $? >> test/llvm/"$name".out
            fi
            diff test/llvm/"$name".out test/llvm/"$name".ans
            echo "    \e[32mtest ok\e[0m"
        done
    elif [ "$3" = "show" ]; then
        cat test/llvm/"$2".ll
    else
        echo "\e[34mbuild compiler\e[0m"
        zsh gradlew installDist
        echo "\e[34mmxc --llvm\e[0m"
        time mxc --llvm test/llvm/"$2".mx
        echo "\e[34mllc\e[0m"
        llc test/llvm/"$2".ll
        echo "\e[34mgcc -no-pie\e[0m"
        gcc -o test/llvm/"$2" test/llvm/"$2".s test/llvm/builtin_functions.s -no-pie
        echo "\e[34mexecute\e[0m"
        if test -f test/llvm/"$2".in; then
            result=$(test/llvm/"$2" < test/llvm/"$2".in || echo exit code: $?)
        else
            result=$(test/llvm/"$2" || echo exit code: $?)
        fi
        echo "$result"
        echo "$result" > test/llvm/"$2".out
        echo "\e[34mdiff\e[0m"
        diff test/llvm/"$2".out test/llvm/"$2".ans
    fi
fi
