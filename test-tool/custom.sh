#!/bin/zsh
set -e

sh gradlew generateBuiltin installDist

typeset testtool=$(dirname "$0")
typeset cases="$testtool/cases"
typeset llvmcases="$cases/llvm"
typeset built="$testtool/builtin.s"

typeset cyan='\033'\[36m
typeset blue='\033'\[34m
typeset green='\033'\[32m
typeset default='\033'\[0m

generateBuiltin() {
    echo $blue"generate built-in functions"$default
    gcc -S src/main/resources/builtin.c -o "$built"
}

buildLLVM64() {
    typeset current="$1"
    typeset detail="$2"
    typeset indent="$3"
    if [ "$detail" = "true" ]; then echo $blue$indent"mxc --llvm64"$default; fi
    typeset TIMEFMT=$indent"compile time: %*E"
    time (mxc --llvm64 --steps --info "$current.mx"; echo -n $cyan) && echo -n $default
    if [ "$detail" = "true" ]; then echo $blue$indent"llc"$default; fi
    llc $current.ll
    if [ "$detail" = "true" ]; then echo $blue$indent"gcc -no-pie"$default; fi
    gcc -o "$current.bin" "$current.s" "$built" -no-pie
    # echo $green$indent"build successful"$default
}

execute64() {
    typeset current="$1"
    if test -f "$current.in"; then
        if [ "$2" = "stdout" ]; then
            "$current.bin" < "$current.in" || echo exit code: $?
        else
            "$current.bin" < "$current.in" > "$current.out" || echo exit code: $? >> "$current.out"
            if [ "$2" = "both" ]; then cat "$current.out"; fi
        fi
    else
        if [ "$2" = "stdout" ]; then
            "$current.bin" || echo exit code: $?
        else
            "$current.bin" > "$current.out" || echo exit code: $? >> "$current.out"
            if [ "$2" = "both" ]; then cat "$current.out"; fi
        fi
    fi
}

if [ "$1" = "llvm" ]; then
    if [ "$2" = "all" ]; then
        generateBuiltin
        for name in $(cat $llvmcases/list.txt); do
            echo "\e[34mcase $name:\e[0m"
            current="$llvmcases/$name"
            buildLLVM64 "$current" false "    "
            execute64 "$current" outfile
            diff "$current.out" "$current.ans"
        done
    else
        generateBuiltin
        if [ "$2" = "test" ]; then typeset current="$testtool/test";
        else typeset current="$llvmcases/$2"; fi
        if [ "$3" = "debug" ]; then typeset output=stdout;
        else typeset output=both; fi
        buildLLVM64 "$current" true ""
        execute64 "$current" $output
        if [ "$3" != "debug" ]; then
            echo "\e[34mdiff\e[0m"
            diff "$current.out" "$current.ans"
        fi
    fi
fi
