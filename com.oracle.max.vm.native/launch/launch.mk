#
# Copyright (c) 2019, APT Group, School of Computer Science,
# The University of Manchester. All rights reserved.
# Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

PROJECT = ../../..

MAIN = maxvm

SOURCES = maxvm.c

SOURCE_DIRS = jni platform share substrate launch

include $(PROJECT)/platform/platform.mk
ifeq ($(OS),windows)
    MAIN = maxvm.exe #some cp implementations for windows require .exe at the end, while other work without it

endif
include $(PROJECT)/share/share.mk

all : $(MAIN)
	$(AT) mkdir -p $(PROJECT)/generated/$(OS)
	$(AT) cp -f $(PROJECT)/build/$(OS)/launch/$(MAIN) $(PROJECT)/generated/$(OS)
	$(AT) # The command below was needed for JDK 1.6.0_17 on Mac OS X:
	$(AT) # if [ $(OS) = "darwin" ]; then $(PROJECT)/../bin/mod-macosx-javalib.sh $(PROJECT)/generated/$(OS) $(JAVA_HOME); fi
