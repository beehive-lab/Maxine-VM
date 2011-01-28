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



