#!/bin/zsh
set -e

sh gradlew generateBuiltin installDist

typeset testtool=$(dirname "$0")
typeset cases="$testtool/cases"
typeset built="$testtool/builtin.s"

typeset cyan='\033'\[36m
typeset blue='\033'\[34m
typeset green='\033'\[32m
typeset default='\033'\[0m

single() {
	echo "\e[34mbuild $name\e[0m"
	mxc --debug "$current.mx" &> "$testtool/test.log" || (cat "$testtool/test.log" && return 1)
	echo "\e[34msimulate $name\e[0m"
	if [ -f "$current.in" ]; then
		ravel --input-file="$current.in" --output-file="$current.out" build/resources/main/builtin.s "$current.s" &> /dev/null
	else
		ravel --output-file="$current.out" build/resources/main/builtin.s "$current.s" &> /dev/null
	fi
	echo "\e[34mdiff $name\e[0m"
	diff "$current.out" "$current.ans" || cat "$current.out" # && return 1
	echo "\e[32msuccess $name\e[0m"
	echo 
}

if [ "$1" = "all" ]; then
	for nam in $(cat $cases/list.txt); do
		typeset name="$nam"
		typeset current="$cases/$name"
		single
	done
else
	if [ "$1" = "test" ]; then
		typeset current="$testtool/test"
		typeset name="test"
	else
		typeset current="$cases/$1"
		typeset name="$1"
	fi
	single
fi
