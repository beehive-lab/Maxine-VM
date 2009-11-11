#!/bin/bash

test -n "$JUNIT4_CP"         || export JUNIT4_CP=/proj/maxwell/bin/junit4.jar
test -n "$MAXINE_HOME"       || export MAXINE_HOME=.
test -n "$SPECJVM_CLASSPATH" || export SPECJVM_CLASSPATH=/proj/maxwell/specjvm98.zip
test -n "$DACAPO_JAR"        || export DACAPO_JAR=/proj/maxwell/dacapo-2006-10-MR2.jar
test -n "$RESULTS_DIR"       || export RESULTS_DIR=$MAXINE_HOME/vee2010-results

mkdir -p $MAXINE_HOME/vee2010-results

TIMING_RUNS=3

function max_cp() {
        echo ${MAXINE_HOME}/$1/bin
}

C1X_TUNING='-J-d64 -J/a-XX:MaxPermSize=250m -J/a-Xms2g -J/a-Xmx2g'
C1X_ASSERTS='-XX:+IRChecking'
C1X_NO_ASSERTS='-XX:-IRChecking -XX:TraceLinearScanLevel=0'
C1X_XIR='-XX:+GenerateLIRXIR -XX:+GenerateUnresolvedLIRXIR'

function c1x-opt() {
    file=$RESULTS_DIR/$2-c1x${1}
    echo '-->' $file
    $MAXINE_HOME/bin/max $C1X_TUNING c1x -nowarn $C1X_NO_ASSERTS -search-cp=$5 $4 > ${file}

    file=$RESULTS_DIR/$2-c1x${1}x
    echo '-->' $file
    $MAXINE_HOME/bin/max $C1X_TUNING c1x -nowarn $C1X_NO_ASSERTS $C1X_XIR -timing=$3 -search-cp=$5 $4 > ${file}
}

function c1x() {
    c1x-opt 0 "$@"
    c1x-opt 1 "$@"
    c1x-opt 2 "$@"
}

echo JDK16
c1x jdk16 1 "^java"

echo Maxine
c1x maxine 1 "^com.sun.max"

echo C1X
c1x c1x -timing=25 "^com.sun.c1x"

echo SpecJVM98
c1x specjvm98 25 "^spec." $SPECJVM_CLASSPATH

echo DaCapo
c1x dacapo 25 "^dacapo ^org.eclipse ^EDU.purdue ^antlr ^net.sourceforge.pmd avalon batik ^org.apache.fop ^org.apache.xerces" $DACAPO_JAR 
