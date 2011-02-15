#!/bin/bash
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------

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
