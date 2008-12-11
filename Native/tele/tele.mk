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

PROJECT = ../../..

LIB = tele

include $(PROJECT)/platform/platform.mk
include $(PROJECT)/tele/$(OS)/$(OS).mk

SOURCES = $(OS_SOURCES) address.c arithmeticException.c boxedMemory.c boxedPointer.c c.c log.c tele.c mutex.c isa.c $(ISA).c jni.c loophole.c platform.c relocation.c virtualMemory.c

SOURCE_DIRS = tele tele/$(OS) jni platform prototype share

include $(PROJECT)/share/share.mk

all : $(LIBRARY)
