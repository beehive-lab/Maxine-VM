#
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

# Makefile for all of Maxine's native code with support for prototyping (including image building),
# testing, inspecting (browsing, profiling and debugging) and running a Maxine VM.

include platform/platform.mk

all : hosted substrate launch tele javatest

hosted : build/$(OS)/hosted/makefile
	(cd build/$(OS)/hosted; $(MAKE) all)

substrate : build/$(OS)/substrate/makefile
	(cd build/$(OS)/substrate; $(MAKE) all)
 
launch : substrate build/$(OS)/launch/makefile
	(cd build/$(OS)/launch; $(MAKE) all)
	
tele : build/$(OS)/tele/makefile
	(cd build/$(OS)/tele; $(MAKE) all)
 
javatest : build/$(OS)/javatest/makefile
	(cd build/$(OS)/javatest; $(MAKE) all)

build/$(OS)/hosted/makefile : hosted/hosted.mk
	mkdir -p build/$(OS)/hosted
	cp -f hosted/hosted.mk build/$(OS)/hosted/makefile
 
build/$(OS)/substrate/makefile : substrate/substrate.mk
	mkdir -p build/$(OS)/substrate
	cp -f substrate/substrate.mk build/$(OS)/substrate/makefile
 
build/$(OS)/launch/makefile : launch/launch.mk
	mkdir -p build/$(OS)/launch
	cp -f launch/launch.mk build/$(OS)/launch/makefile
 
build/$(OS)/tele/makefile : tele/tele.mk
	mkdir -p build/$(OS)/tele
	cp -f tele/tele.mk build/$(OS)/tele/makefile

build/$(OS)/javatest/makefile : javatest/javatest.mk
	mkdir -p build/$(OS)/javatest
	cp -f javatest/javatest.mk build/$(OS)/javatest/makefile
 
clean :
	rm -rf build/$(OS)

platform :
	echo $(PLATFORM)
.PHONY: clean hosted substrate launch tele javatest platform

