#!/bin/bash
#
# This script builds the Maxine projects required to run the Maxine
# autotests (i.e the JUnit test classes named AutoTest.java).
#
# The following environment variables must be set:
#
# JAVA_HOME:
#    The JDK installation directory which has a 'bin' subdirectory containing
#    the 'java' and 'javac' executables. If not on MacOS, then ${JAVA_HOME}/lib/tools.jar must exist.
# JUNIT_CLASSES:
#    The path to junit-4.1.jar. If you have Eclipse installed, then the easiest way to set
#    this is JUNIT_CLASSES=`find <path to eclipse>/plugins -name 'junit-4.1.jar'`
#
# The following environment variables may optionally be set:
# 
# TEST_DIR:
#    The directory in which the tests will be run and results written to.
#    Default: `pwd`
# SOURCE_HG_REPO:
#    The HG repository to pull from.
#    Default: /proj/maxwell/hg/maxine
# TIMEOUT:
#    The number of seconds a single Autotest is run before being killed.
#    Default: 600
# MAILTO, MAILFROM:
#    Required to be notified of the results by email. Both of these should be valid email addresses if set.
#    Default: <not set>
# CONSOLE_LOG_FILE:
#    If the tests are run, then the verbose output of this script (i.e. set -x) is copied to this file
#    just before the script exits
#    Default: <not set>
# URL:
#    The URL prefix of where the results will be available.
#    Default: <not set>

# Non-optional variables must be set
test "X${JAVA_HOME}" != "X" || { echo "JAVA_HOME must be set"; exit; }
test "X${JUNIT_CLASSES}" != "X" || { echo "JUNIT_CLASSES must be set"; exit; }

# Undefined optional variables are set to defaults
test "X${TEST_DIR}" != "X" || TEST_DIR=`pwd`
test "X${TIMEOUT}" != "X" || TIMEOUT=600
test "X${SOURCE_HG_REPO}" != "X" || SOURCE_HG_REPO="/proj/maxwell/hg/maxine"

TEST_HG_REPO=${TEST_DIR}/workspace

# Verbose execution
set -x

# The set of projects whose sources are to be compiled. The order of this list must
# reflect any dependencies between the projects (i.e. if x depends on y, then y must
# precede x in the list).
PROJECTS=( \
    Base:src,test,shell   \
    Assembler:src,test    \
    VM:src,test           \
    VMDI:src              \
    Tele:src              \
    JDWP:src              \
    TeleJDWP:src          \
    Inspector:src,test    \
)

JDK_TOOLS=${JAVA_HOME}/lib/tools.jar

CLASSPATH=${JDK_TOOLS}
CLASSPATH=${CLASSPATH}:${JUNIT_CLASSES}

for PROJECT in ${PROJECTS[@]}; do
    NAME=`echo $PROJECT | sed 's/:.*//g'`
    CLASSPATH=${CLASSPATH}:${TEST_HG_REPO}/${NAME}/bin
done
CLASSPATH=${CLASSPATH}:/net/maxwell/export/proj/maxwell/software/JCK-runtime-6/classes

JAVA_ARGS="-ea -Xss2m -Xms1G -Xmx2G"    
JAVA="${JAVA_HOME}/bin/java ${JAVA_ARGS} -classpath ${CLASSPATH}"
JAVAC="${JAVA_HOME}/bin/javac -g -J-Xmx1024m -classpath ${CLASSPATH} -d ../bin"

# Use -d64 if the JVM supports it (required to make 64-bit libprototype.so load successfully)
if [ "X`${JAVA} | grep '\-d64'`" != "X" ];
then
    JAVA="${JAVA} -d64"
fi

REL_OUTPUT_DIR=test-output/`date +%F-%H%M`
OUTPUT_DIR=${TEST_DIR}/${REL_OUTPUT_DIR}
COMPILER_FILE=${OUTPUT_DIR}/COMPILER
NATIVE_FILE=${OUTPUT_DIR}/NATIVE
SUMMARY_FILE=${OUTPUT_DIR}/SUMMARY
LAST_CHANGESET_FILE=${TEST_DIR}/runtests-changeset
LOCK_FILE=${TEST_DIR}/runtests.lock

START_TIME=`date`

MAKE=gmake

# Check to see whether LOCK_FILE exists in the working directory
# If it does, the tests are already being run; we just exit.

if [ -e ${LOCK_FILE} ] ;
then
    echo "Tests already running [remove ${LOCK_FILE} if not]"
    exit
fi

touch ${LOCK_FILE}

# Play nicely with the poor unfortunate user whose machine we are using

renice +19 $$

if [ -d ${TEST_HG_REPO} ];
then
    if [ ! -d ${TEST_HG_REPO}/.hg ]; then
        echo "${TEST_HG_REPO} does not look a Mercurial repository - missing .hg subdirectory"
        exit
    fi
    # HG repo exists: pull to get recent changes
    (cd ${TEST_HG_REPO} ; hg pull -u )
else
    # HG repo does not exist: clone to create it
    hg clone ${SOURCE_HG_REPO} ${TEST_HG_REPO}
fi

if [ -e ${LAST_CHANGESET_FILE} ] ;
then
    LAST_CHANGESET=`cat ${LAST_CHANGESET_FILE}`
    TIP_CHANGESET=`hg -R ${TEST_HG_REPO} tip --template '{node|short}'`
    
    if [ "x${LAST_CHANGESET}" = "x${TIP_CHANGESET}" ]; then
        echo "No changes from last test run"
        rm ${LOCK_FILE}
        exit
    else
        echo ${TIP_CHANGESET} >${LAST_CHANGESET_FILE}
    fi
fi

if [ ! -e ${OUTPUT_DIR} ] ;
then
    mkdir -p ${OUTPUT_DIR}
fi

# Link latest -> the output directory we just created
rm ${TEST_DIR}/latest
ln -s ${OUTPUT_DIR} ${TEST_DIR}/latest

rm -f ${COMPILER_FILE}
rm -f ${SUMMARY_FILE}
rm -f ${NATIVE_FILE}
echo -e "This is ${OUTPUT_DIR}" >${SUMMARY_FILE}
echo -e "JAVA_HOME=${JAVA_HOME}" >>${SUMMARY_FILE}
echo -e "WORKSPACE=${TEST_HG_REPO}" >>${SUMMARY_FILE}

COMPILATION_RESULT=0
if [ "x$SKIP_JAVAC" = "x" ]; then

    for PROJECT in ${PROJECTS[@]}; do
        NAME=`echo $PROJECT | sed 's/:.*//g'`
        rm -rf ${TEST_HG_REPO}/${NAME}/bin
        mkdir ${TEST_HG_REPO}/${NAME}/bin
        
        SRC_DIRS=`echo $PROJECT | sed 's/.*://g' | tr ',' ' '`
        for SRC_DIR in ${SRC_DIRS} ; do
            ( cd ${TEST_HG_REPO}/${NAME}/${SRC_DIR}
              pwd >> ${COMPILER_FILE}
              ${JAVAC} `find . -name *.java | grep -v SCCS` >>${COMPILER_FILE} 2>&1 );
            RESULT=$?
            if [ $RESULT -ne 0 ]; then
                COMPILATION_RESULT=$RESULT;
                
                # Break out of the 'for PROJECT ...' loop
                break 2;
            fi
        done
    done

    # Filter out the warnings about using Sun proprietary API (which cannot be disabled unfortunately)
    awk 'BEGIN { c=-1} /is Sun proprietary API/ { c=2; } { if (c-- >= 0) next; print; }' < ${COMPILER_FILE} >${COMPILER_FILE}.$$;
    mv ${COMPILER_FILE}.$$ ${COMPILER_FILE}
fi

# build native:
if [ $COMPILATION_RESULT -eq 0 ]; then
( cd ${TEST_HG_REPO}/Native
  ${MAKE} clean >>${NATIVE_FILE} 2>&1 
  ${MAKE} >>${NATIVE_FILE} 2>&1 )
fi

if [ $COMPILATION_RESULT -ne 0 ]; then
    echo -e "There were compilation errors: see ${COMPILER_FILE} and ${NATIVE_FILE} for details\n" >>${SUMMARY_FILE}
else

    ############## Maxine Tester #############
    (
      cd ${OUTPUT_DIR}
      ${JAVA} test.com.sun.max.vm.MaxineTester \
          -output-dir=maxine-tester \
          -java-tester-concurrency=1 \
          -image-build-timeout=600 \
          -java-tester-timeout=30 \
          -java-run-timeout=50 >>${SUMMARY_FILE} 2>&1
      # Remove the VM image, executable and shared libraries
      rm `find maxine-tester -name '*.so' -o -name 'maxine.vm' -o -name 'maxvm' -o -name '*.jar'`
    );
    
    ############## JUNIT AUTOTESTS #############
    if [ "x$SKIP_AUTOTESTS" = "x" ]; then
        # run every AutoTest class
        for PROJECT in ${PROJECTS[@]}; do
        (
            NAME=`echo $PROJECT | sed 's/:.*//g'`
            if [ ! -d ${TEST_HG_REPO}/${NAME}/test ]; then
                continue;
            fi
            cd ${TEST_HG_REPO}/${NAME}/test;
            for t in `find . -name AutoTest.java | sort` ; do
                JAVA_TEST=`echo $t | sed s!\^./!! | sed s!/!.!g | sed s/.java$//`
                
                # Get any test specific args
                JAVA_TEST_ARGS_FILE=`echo $t | sed s/.java$/.args/`
                if [ -f ${JAVA_TEST_ARGS_FILE} ] ;
                then
                JAVA_TEST_ARGS=" `cat ${JAVA_TEST_ARGS_FILE}`"
                else
                JAVA_TEST_ARGS=""
                fi
                
                echo ${JAVA_TEST}
                RUNNING_FILE="${OUTPUT_DIR}/running_${NAME}_${JAVA_TEST}"
                OUTPUT_FILE="${OUTPUT_DIR}/${NAME}_${JAVA_TEST}"
                touch ${RUNNING_FILE}
                
                # Make asynchronous subshells run in a separate process group
                set -m
                
                ( ${JAVA} ${JAVA_TEST_ARGS} org.junit.runner.JUnitCore ${JAVA_TEST} >${OUTPUT_FILE} 2>&1 ;
                rm -f ${RUNNING_FILE}
                ) &
                SUBSHELL_PID=$!
                
                ( sleep ${TIMEOUT}
                if [ -e ${RUNNING_FILE} ] ;
                    then
                    /usr/bin/kill -9 -${SUBSHELL_PID}
                    echo -e "\n\nTimeout - killed" >> ${OUTPUT_FILE}
                fi
                ) &
                TIMEOUT_PID=$!
                
                # Make asynchronous subshells run in the current process group
                set +m
                
                wait ${SUBSHELL_PID}
                if [ -e ${RUNNING_FILE} ]; then
                    # subshell process group was killed
                    rm -f ${RUNNING_FILE}
                else
                    # VM exited normally, abandon the timeout
                    /usr/bin/kill -9 -${TIMEOUT_PID}
                fi
                
		            echo >> ${SUMMARY_FILE}
                echo -e "${NAME}_${JAVA_TEST} [${TEST_DIR}/${REL_OUTPUT_DIR}/${NAME}_${JAVA_TEST}]\n  \c" >>${SUMMARY_FILE}
                egrep "Tests run:|^OK \([0-9]+ tests?\)" ${OUTPUT_FILE} >>${SUMMARY_FILE} || echo "Did not complete" >>${SUMMARY_FILE}
                echo -e "  \c" >>${SUMMARY_FILE}
                egrep "^Time: [0-9\.,]+$" ${OUTPUT_FILE} >>${SUMMARY_FILE} || echo "[No timing info found]" >>${SUMMARY_FILE}
                
            done
        )
        done
    fi

fi

echo "" >>${SUMMARY_FILE}
echo "Start: ${START_TIME}" >>${SUMMARY_FILE}
echo "End:   `date`" >>${SUMMARY_FILE}

if [ "x${URL}" != "x" ]; then
    cat ${SUMMARY_FILE} | sed s@${TEST_DIR}@${URL}@g > ${SUMMARY_FILE}.$$
    mv ${SUMMARY_FILE}.$$ ${SUMMARY_FILE}
fi

cat ${SUMMARY_FILE}

# Send mail containing the summary only if both MAILTO and MAILFROM are set
if [ "X${MAILTO}" != "X" ];
then
    if [ "X${MAILFROM}" != "X" ];
    then
        echo -e "From: ${MAILFROM}\nSubject: Automated Maxine test [`uname -a`]\n" | cat - ${SUMMARY_FILE} | mail -t ${MAILTO}
    fi
fi

if [ "X${CONSOLE_LOG_FILE}" != "X" ] ;
then
    cp ${CONSOLE_LOG_FILE} ${TEST_DIR}/latest/CONSOLE
fi

# Release the lock
rm ${LOCK_FILE}
