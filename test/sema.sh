if [ "$1" = "all" ]; then
    (cd assignment/local-judge && python judge.py)
else
    zsh gradlew installDist || return 98
    cp assignment/local-judge/testcase/sema/"$1"-package/"$1"-"$2".mx temp/test.mx || return 98
    cat --number temp/test.mx || return 98
    echo
    mxc temp/test.mx --semantic
fi
