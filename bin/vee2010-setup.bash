#!/bin/bash

export VEE_TEST_OPTIONS="-timing-runs=5 -fail-fast=f -tests=specjvm98,dacapo -specjvm98=/proj/maxwell/specjvm98.zip -dacapo=/proj/maxwell/dacapo-2006-10-MR2.jar"

# reference config with CPS compiler
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=jit,std -maxvm-config-alias=java

# configs with C1X as JIT compiler
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x0
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x0x
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x1
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x1x
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x2
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x2x
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x3
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=jit-c1x3x

# configs with C1X as optimizing compiler
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x0
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x0x
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x1
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x1x
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x2
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x2x
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x3
# max gate ${VEE_TEST_OPTIONS} -maxvm-configs=std -maxvm-config-alias=opt-c1x3x



