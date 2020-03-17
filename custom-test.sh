set -e

if [ "$1" = "llvm" ]; then
    echo "\e[34mgenerate built-in functions\e[0m"
    gcc -S src/main/resources/builtin_functions.c -o test/llvm/builtin_functions.s
    if [ "$2" = "all" ]; then
        echo "\e[34mbuild compiler\e[0m"
        zsh gradlew installDist
        for name in $(cat test/llvm/list.txt); do
            full="test/llvm/$name"
            echo "\e[34mtestcase $name:\e[0m"
            \time -f "    compile time: %E" mxc --llvm64 $full.mx
            llc $full.ll
            gcc -o $full $full.s test/llvm/builtin_functions.s -no-pie
            echo "    \e[32mbuild successful\e[0m"
            if test -f $full.in; then
                $full < $full.in > $full.out || echo exit code: $? >> $full.out
            else
                $full > $full.out || echo exit code: $? >> $full.out
            fi
            diff $full.out $full.ans
            echo "    \e[32mtest ok\e[0m"
        done
    elif [ "$3" = "show" ]; then
        cat test/llvm/"$2".ll
    else
        if [ "$2" = "test" ]; then
            full="test/test"
        else
            full="test/llvm/$2"
        fi
        echo "\e[34mbuild compiler\e[0m"
        zsh gradlew installDist
        echo "\e[34mmxc --llvm64\e[0m"
        time mxc --llvm64 $full.mx
        echo "\e[34mllc\e[0m"
        llc $full.ll
        echo "\e[34mgcc -no-pie\e[0m"
        gcc -o $full $full.s test/llvm/builtin_functions.s -no-pie
        if [ "$3" = "debug" ]; then
            echo "\e[34mdebug\e[0m"
            if test -f $full.in; then
                $full < $full.in || echo exit code: $?
            else
                $full || echo exit code: $?
            fi
        else
            echo "\e[34mexecute\e[0m"
            if test -f $full.in; then
                result=$($full < $full.in || echo exit code: $?)
            else
                result=$($full || echo exit code: $?)
            fi
            echo "$result"
            echo "$result" > $full.out
            echo "\e[34mdiff\e[0m"
            diff $full.out $full.ans
        fi
    fi
fi
