#!/bin/bash

export VEE_TEST_OPTIONS="-timing-runs=5 -fail-fast=f -tests=specjvm98,dacapo -specjvm98=/proj/maxwell/specjvm98.zip -dacapo=/proj/maxwell/dacapo-2006-10-MR2.jar"

if [ x"$1" = xjit ]; then
    shift

    echo jit-c1x0
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x0 > /tmp/vee2010-results/jit-c1x0

    echo jit-c1x0x
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x0x > /tmp/vee2010-results/jit-c1x0x

    echo jit-c1x1
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x1 > /tmp/vee2010-results/jit-c1x1

    echo jit-c1x1x
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x1x > /tmp/vee2010-results/jit-c1x1x

    echo jit-c1x2
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x2 > /tmp/vee2010-results/jit-c1x2
    
    echo jit-c1x2x
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x2x > /tmp/vee2010-results/jit-c1x2x

fi

if [ x"$1" = xopt ]; then

    echo opt-c1x0
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x0 > /tmp/vee2010-results/opt-c1x0

    echo opt-c1x0x
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x0x > /tmp/vee2010-results/opt-c1x0x
    
    echo opt-c1x1
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x1 > /tmp/vee2010-results/opt-c1x1

    echo opt-c1x1x
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x1x > /tmp/vee2010-results/opt-c1x1x

    echo opt-c1x2
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x2 > /tmp/vee2010-results/opt-c1x2

    echo opt-c1x2x
    max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x2x > /tmp/vee2010-results/opt-c1x2x

fi
