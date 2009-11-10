#!/bin/bash

#export JUNIT4_CP=/proj/maxwell/bin/junit4.jar
#export MAXINE_HOME=~/maxine
#export MAXINE_OUT=${MAXINE_HOME}
#export SPECJVM_CLASSPATH=/proj/maxwell/specjvm98-noinput
#export CHECKSTYLE_JAR=/proj/maxwell/bin/checkstyle-4.jar

export SCIMARK_CP=/project/titzer/scimark2/bin

if [ x"$SPECJVM_NOINPUT_CLASSPATH" = x ]; then
	echo Please set SPECJVM_NOINPUT_CLASSPATH
	exit
fi

function max_cp() {
	if [ x"$MAXINE_IDE" = xINTELLIJ ]; then	
	        echo ${MAXINE_HOME}/out/production/$1
	else
	        echo ${MAXINE_HOME}/$1/bin
	fi
}

C1X_CP="$(max_cp VM):${MAXINE_HOME}/VM/classes:$(max_cp Base):$(max_cp CRI):$(max_cp C0X):$(max_cp C1X):$(max_cp Assembler):${JUNIT4_CP}:$SPECJVM_NOINPUT_CLASSPATH:${SCIMARK_CP}"

C1X_TUNING='-XX:MaxPermSize=250m -Xms2g -Xmx2g'
C1X_ASSERTS='-XX:+IRChecking'
C1X_NO_ASSERTS='-XX:-IRChecking -XX:TraceLinearScanLevel=0 -XX:+PrintVEEMetrics -XX:+PrintMetrics'
C1X_XIR='-XX:+GenerateLIRXIR -XX:+GenerateUnresolvedLIRXIR'

function c1x-opt() {
    optlevel="$1"
    benchmark="$2"
    warmup="$3"
    timing="$4"
    classes="$5"
    
    file=/tmp/vee2010-results/static-${benchmark}-${optlevel}.txt
    echo '-->' $file
    java -d64 $C1X_TUNING -cp $C1X_CP test.com.sun.max.vm.compiler.c1x.C1XTest $C1X_NO_ASSERTS -warmup=${warmup} -timing=${timing} -c1x-optlevel=${optlevel} ${classes} > ${file}

    file=/tmp/vee2010-results/static-${benchmark}-${optlevel}x.txt
    echo '-->' $file
    java -d64 $C1X_TUNING -cp $C1X_CP test.com.sun.max.vm.compiler.c1x.C1XTest $C1X_NO_ASSERTS $C1X_XIR -warmup=${warmup} -timing=${timing} -c1x-optlevel=${optlevel} ${classes} > ${file}
}

function c1x() {
#    c1x-opt 0 "$1" "$2" "$3" "$4"
    c1x-opt 1 "$1" "$2" "$3" "$4"
#    c1x-opt 2 "$1" "$2" "$3" "$4"
    c1x-opt 3 "$1" "$2" "$3" "$4"
}

echo JDK16
#c1x jdk16 5 10 "^java"

echo Maxine
#c1x maxine 5 10 "^com.sun.max"

echo C1X
#c1x c1x 10 25 "^com.sun.c1x"

echo SpecJVM98
#c1x specjvm98 10 25 "^spec."

echo SpecJVM98
c1x scimark 25 50 "^jnt."

echo DaCapo
