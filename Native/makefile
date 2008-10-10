#
# Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
# 
# Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product 
# that is described in this document. In particular, and without limitation, these intellectual property 
# rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or 
# more additional patents or pending patent applications in the U.S. and in other countries.
# 
# U.S. Government Rights - Commercial software. Government users are subject to the Sun 
# Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its 
# supplements.
# 
# Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or 
# registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks 
# are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the 
# U.S. and other countries.
#
# UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open 
# Company, Ltd.
#

# Makefile for all of Maxine's native code with support for prototyping (including image building),
# testing, inspecting (browsing, profiling and debugging) and running a Maxine VM.

include platform/platform.mk

all : prototype substrate launch inspector javatest

prototype : build/$(OS)/prototype/makefile
	(cd build/$(OS)/prototype; gmake all)

substrate : build/$(OS)/substrate/makefile
	(cd build/$(OS)/substrate; gmake all)
 
launch : substrate build/$(OS)/launch/makefile
	(cd build/$(OS)/launch; gmake all)
	
inspector : build/$(OS)/inspector/makefile
	(cd build/$(OS)/inspector; gmake all)
 
javatest : build/$(OS)/javatest/makefile
	(cd build/$(OS)/javatest; gmake all)

build/$(OS)/prototype/makefile : prototype/prototype.mk
	mkdir -p build/$(OS)/prototype
	cp -f prototype/prototype.mk build/$(OS)/prototype/makefile
 
build/$(OS)/substrate/makefile : substrate/substrate.mk
	mkdir -p build/$(OS)/substrate
	cp -f substrate/substrate.mk build/$(OS)/substrate/makefile
 
build/$(OS)/launch/makefile : launch/launch.mk
	mkdir -p build/$(OS)/launch
	cp -f launch/launch.mk build/$(OS)/launch/makefile
 
build/$(OS)/inspector/makefile : inspector/inspector.mk
	mkdir -p build/$(OS)/inspector
	cp -f inspector/inspector.mk build/$(OS)/inspector/makefile

build/$(OS)/javatest/makefile : javatest/javatest.mk
	mkdir -p build/$(OS)/javatest
	cp -f javatest/javatest.mk build/$(OS)/javatest/makefile
 
clean :
	rm -rf build/$(OS)
 
.PHONY: clean prototype substrate launch inspector javatest

