#!/bin/zsh
set -e

sh gradlew generateBuiltin installDist
typeset testtool=$(dirname "$0")
typeset cases="$testtool/optim"

echo -n > "$testtool/test.score"

for nam in $(cat "$cases/list.txt"); do
    printf "%-20s" $nam
    cp "$cases/$nam.mx" "$testtool/test.mx"
    cp "$cases/$nam.in" "$testtool/test.in"
    cp "$cases/$nam.ans" "$testtool/test.ans"
    mxc "$testtool/test.mx"
    (cd $testtool && ravel --oj-mode --enable-cache &> "test.detail")
    (cd $testtool && python "time.py" $nam)
    (cd $testtool && diff -q -w "test.out" "test.ans")
    echo
done

(cd $testtool && python "score.py")
