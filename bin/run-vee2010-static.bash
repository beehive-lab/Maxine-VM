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
#

test -n "$JUNIT4_CP"         || export JUNIT4_CP=/proj/maxwell/bin/junit4.jar
test -n "$MAXINE_HOME"       || export MAXINE_HOME=.
test -n "$SPECJVM_CLASSPATH" || export SPECJVM_CLASSPATH=/proj/maxwell/specjvm98.zip
test -n "$DACAPO_JAR"        || export DACAPO_JAR=/proj/maxwell/dacapo-2006-10-MR2.jar
test -n "$RESULTS_DIR"       || export RESULTS_DIR=$MAXINE_HOME/vee2010-results-static

mkdir -p $RESULTS_DIR

# Class path for class files in http://math.nist.gov/scimark2/scimark2src.zip
test -n "$SCIMARK_CP" || export SCIMARK_CP=/proj/maxwell/scimark-2.0

# Class path for SPEC JVM 98 with the spec/benchmarks/_213_javac/input directory removed
test -n "$SPECJVM_NOINPUT_CP" || export SPECJVM_NOINPUT_CP=/proj/maxwell/specjvm98-noinput

function max_cp() {
	if [ x"$MAXINE_IDE" = xINTELLIJ ]; then	
	        echo ${MAXINE_HOME}/out/production/$1
	else
	        echo ${MAXINE_HOME}/$1/bin
	fi
}


C1X_CP="$(max_cp VM):${MAXINE_HOME}/VM/classes:$(max_cp Base):$(max_cp CRI):$(max_cp C0X):$(max_cp C1X):$(max_cp MaxineC1X):$(max_cp Assembler):$(max_cp VM):$(max_cp CPS):${JUNIT4_CP}:$SPECJVM_NOINPUT_CP:${SCIMARK_CP}:${DACAPO_JAR}"

C1X_TUNING='-XX:MaxPermSize=250m -Xms2g -Xmx2g'

C1X_ASSERTS='-C1X:+IRChecking'
C1X_NO_ASSERTS='-C1X:-IRChecking -C1X:TraceLinearScanLevel=0 -C1X:+PrintMetrics'
C1X_XIR='-C1X:+UseXIR'

function c1x-opt() {
    optlevel="$1"
    benchmark="$2"
    warmup="$3"
    timing="$4"
    classes="$5"
    
    file=$RESULTS_DIR/static-${benchmark}-${optlevel}x.txt
    echo '-->' $file
    cmd="java -da -d64 $C1X_TUNING -cp $C1X_CP test.com.sun.max.vm.compiler.c1x.C1XTest $C1X_NO_ASSERTS $C1X_XIR -warmup=${warmup} -timing=${timing} -C1X:OptLevel=${optlevel} ${classes}"
    echo $cmd > ${file}
    $cmd >> ${file}

    file=$RESULTS_DIR/static-${benchmark}-${optlevel}.txt
    echo '-->' $file
    cmd="java -da -d64 $C1X_TUNING -cp $C1X_CP test.com.sun.max.vm.compiler.c1x.C1XTest $C1X_NO_ASSERTS -warmup=${warmup} -timing=${timing} -C1X:OptLevel=${optlevel} ${classes}"
    echo $cmd > ${file}
    $cmd >> ${file}
}

function c1x() {
    c1x-opt 1 "$@"
    c1x-opt 3 "$@"
}

#echo JDK16
c1x jdk16 5 10 "^java"

echo Maxine
c1x maxine 5 10 "^com.sun.max !sparc"

#echo C1X
c1x c1x 10 25 "^com.sun.c1x"

#echo SpecJVM98
c1x specjvm98 10 25 "^spec."

#echo SciMark 2
c1x scimark 25 50 "^jnt."

#echo DaCapo
#c1x dacapo 10 25 "^dacapo ^org.eclipse ^EDU.purdue ^antlr ^net.sourceforge.pmd avalon batik ^org.apache.fop ^org.apache.xerces"
