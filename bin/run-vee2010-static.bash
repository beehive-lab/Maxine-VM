#!/bin/bash

test -n "$JUNIT4_CP"         || export JUNIT4_CP=/proj/maxwell/bin/junit4.jar
test -n "$MAXINE_HOME"       || export MAXINE_HOME=.
test -n "$SPECJVM_CLASSPATH" || export SPECJVM_CLASSPATH=/proj/maxwell/specjvm98-noinput
test -n "$CHECKSTYLE_JAR"    || export CHECKSTYLE_JAR=/proj/maxwell/bin/checkstyle-4.jar
test -n "$RESULTS_DIR"       || export RESULTS_DIR=$MAXINE_HOME/vee2010-results

mkdir -p $MAXINE_HOME/vee2010-results

TIMING_RUNS=3

function max_cp() {
        echo ${MAXINE_HOME}/$1/bin
}

C1X_CP="$(max_cp VM):${MAXINE_HOME}/VM/classes:$(max_cp Base):$(max_cp CRI):$(max_cp C0X):$(max_cp C1X):$(max_cp Assembler):${JUNIT4_CP}:$SPECJVM_CLASSPATH"

C1X_TUNING='-XX:MaxPermSize=250m -Xms2g -Xmx2g'
C1X_ASSERTS='-XX:+IRChecking'
C1X_NO_ASSERTS='-XX:-IRChecking -XX:TraceLinearScanLevel=0'
C1X_XIR='-XX:+GenerateLIRXIR -XX:+GenerateUnresolvedLIRXIR'

function c1x-opt() {
    file=$RESULTS_DIR/$2-c1x${1}
    echo '-->' $file
    java -d64 $C1X_TUNING -cp $C1X_CP test.com.sun.max.vm.compiler.c1x.C1XTest $C1X_NO_ASSERTS -timing=$3 $4 > ${file}

    file=$RESULTS_DIR/$2-c1x${1}x
    echo '-->' $file
    java -d64 $C1X_TUNING -cp $C1X_CP test.com.sun.max.vm.compiler.c1x.C1XTest $C1X_NO_ASSERTS $C1X_XIR -timing=$3 $4 > ${file}
}

function c1x() {
    c1x-opt 0 "$1" "$2" "$3"
    c1x-opt 1 "$1" "$2" "$3"
    c1x-opt 2 "$1" "$2" "$3"
}

echo JDK16
c1x jdk16 1 "^java"

echo Maxine
c1x maxine 1 "^com.sun.max"

echo C1X
c1x c1x -timing=25 "^com.sun.c1x"

echo SpecJVM98
c1x specjvm98 25 "^spec."

echo DaCapo
