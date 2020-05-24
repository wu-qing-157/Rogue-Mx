#!/bin/zsh
set -e

sh gradlew generateBuiltin installDist
typeset testtool=$(dirname "$0")
typeset cases="$testtool/optim"

for nam in $(cat "$cases/list.txt"); do
    printf "%-20s" $nam
    cp "$cases/$nam.mx" "$testtool/test.mx"
    cp "$cases/$nam.in" "$testtool/test.in"
    mxc "$testtool/test.mx"
    (cd $testtool && ravel --oj-mode --enable-cache &> "test.detail")
    (cd $testtool && python "time.py" $nam)
    echo
done
