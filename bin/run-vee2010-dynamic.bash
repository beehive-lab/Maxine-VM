#!/bin/bash

test -n "$JUNIT4_CP"     || export JUNIT4_CP=/proj/maxwell/bin/junit4.jar
test -n "$MAXINE_HOME"   || export MAXINE_HOME=.
test -n "$SPECJVM98_ZIP" || export SPECJVM98_ZIP=/proj/maxwell/specjvm98.zip
test -n "$DACAPO_JAR"    || export DACAPO_JAR=/proj/maxwell/dacapo-2006-10-MR2.jar
test -n "$RESULTS_DIR"   || export RESULTS_DIR=$MAXINE_HOME/vee2010-results-dynamic

MAX="$MAXINE_HOME/bin/max -J/a-da"

mkdir -p $RESULTS_DIR

export VEE_TEST_OPTIONS="-timing-runs=5 -fail-fast=f -tests=specjvm98,dacapo -specjvm98=$SPECJVM98_ZIP -dacapo=$DACAPO_JAR"

MAXVM_CONFIGS=GC

if [ x"$1" = xjit ]; then
    shift

#    echo jit-c1x0
#    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=jit-c1x0 > $RESULTS_DIR/jit-c1x0

#    echo jit-c1x0x
#    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=jit-c1x0x > $RESULTS_DIR/jit-c1x0x

    echo jit-c1x1
    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=jit-c1x1 > $RESULTS_DIR/jit-c1x1

    echo jit-c1x1x
    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=jit-c1x1x > $RESULTS_DIR/jit-c1x1x

    echo jit-c1x2
    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=jit-c1x2 > $RESULTS_DIR/jit-c1x2
    
    echo jit-c1x2x
    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=jit-c1x2x > $RESULTS_DIR/jit-c1x2x

elif [ x"$1" = xopt ]; then

#    echo opt-c1x0
#    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=opt-c1x0 > $RESULTS_DIR/opt-c1x0

#    echo opt-c1x0x
#    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=opt-c1x0x > $RESULTS_DIR/opt-c1x0x
    
    echo opt-c1x1
    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=opt-c1x1 > $RESULTS_DIR/opt-c1x1

    echo opt-c1x1x
    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=opt-c1x1x > $RESULTS_DIR/opt-c1x1x

#    echo opt-c1x2
#    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=opt-c1x2 > $RESULTS_DIR/opt-c1x2

#    echo opt-c1x2x
#    $MAX gate ${VEE_TEST_OPTIONS} -maxvm-configs=${MAXVM_CONFIGS} -maxvm-config-alias=opt-c1x2x > $RESULTS_DIR/opt-c1x2x
else
    echo "Usage: $0 jit|opt"
fi
