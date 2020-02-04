#
# Copyright (c) 2020, APT Group, Department of Computer Science,
# School of Engineering, The University of Manchester. All rights reserved.
# Copyright (c) 2017-2019, APT Group, School of Computer Science,
# The University of Manchester. All rights reserved.
# Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

# Print verbose output only when MX_VERBOSE is set to y (i.e. mx -V command is used)
ifneq ($(MX_VERBOSE), y)
    AT := @
    MAKE := $(MAKE) -s
endif

# Currently all Maxine target implementations are 64 bit.
# This is where you might change that for the native code; TARGET_WORD_SIZE is interpreted in word.h
# N.B. There are no doubt still assumptions in the code that host and target word size are the same.
# These should be fixed.

TARGET_WORD_SIZE := w64

ifeq ($(LIB), hosted)
    TARGET := HOSTED
endif

ifeq ($(LIB), tele)
    TARGET := TELE
endif

ifeq ($(LIB), jvm)
    TARGET := SUBSTRATE
endif

ifeq ($(LIB), javatest)
    TARGET := SUBSTRATE
endif

ifneq ($(MAIN),)
    TARGET := LAUNCH
endif

# HOSTOS is the platform we are compiling on
HOSTOS = $(shell uname -s)
# TARGETOS is the platform we are compiling for (usually the same as HOSTOS)
# Set TARGETOS explicitly to cross-compile for a different target
# (required for Maxine VE when building tele/inspector)
TARGETOS ?= $(shell uname -s)
# HOSTISA is the architecture we are compiling on
ifeq ($(TARGETOS),Darwin)
    HOSTISA := $(shell uname -p)
endif # Darwin
ifeq ($(TARGETOS),Linux)
    HOSTISA := $(shell uname -m)
endif # Linux
ifeq ($(TARGETOS),SunOS)
    HOSTISA := $(shell isainfo -n)
endif # SunOS
# TARGETISA is the architecture we are compiling for
# Set TARGETISA explicitly to cross-compile for a different target
TARGETISA ?= $(HOSTISA)
# Always use HOSTISA as the TARGETISA for libhosted
ifeq ($(TARGET), HOSTED)
	ifneq ($(TARGETISA), $(HOSTISA))
	    TARGETISA := $(HOSTISA)
	    CROSS_COMPILING := 1
	endif
endif

DARWIN_RELEASE ?= $(shell echo $$(($$(uname -r | cut -d'.' -f 1) > 13 ? 1 : 0)))

ifeq ($(TARGETOS),Darwin)
    OS := darwin
    DARWIN_GCC_MFLAG :=
    ifeq ($(TARGETISA),i386)
        mach := $(shell ls /usr/include/mach/x86_64 2> /dev/null)
        ifneq ($(mach), )
            DARWIN_GCC_MFLAG := -m64
            ISA := amd64
        else
            ifeq ($(DARWIN_RELEASE),1)
                DARWIN_GCC_MFLAG := -m64 -DMaverick
                ISA := amd64
            else
                ISA := ia32
            endif
        endif
    else
       ifeq ($(TARGETISA),powerpc)
           ISA := power
       else
           ISA := $(TARGETISA)
       endif
    endif
endif

ifeq ($(TARGETOS),Linux)
    OS := linux
    ifeq ($(TARGETISA), x86_64)
        ISA := amd64
    endif
    ifeq ($(TARGETISA), x86)
        ISA := ia32
    endif
    ifeq ($(TARGETISA), aarch64)
        ISA := aarch64
    endif
    ifeq ($(TARGETISA), riscv64)
        ISA := riscv64
    endif
    ifeq ($(TARGETISA), armv7l)
        ISA := arm
        OTHER_CFLAGS := -marm -O0 -g -mcpu=cortex-a9
        TARGET_WORD_SIZE := w32
    endif
endif

ifeq ($(TARGETOS),SunOS)
    OS := solaris
    OTHER_CFLAGS := -KPIC  -DLP64
    ifeq ($(TARGETISA),amd64)
        ISA := amd64
        ARCH := amd64
        ARCH_FLAG := "-m64"
        OTHER_CFLAGS := -Kpic
    else
        ifeq ($(TARGETISA),sparcv9)
            ISA := sparc
            ARCH := v9
            ARCH_FLAG := "-m64"
        else
            ISA := sparc
            ARCH := v8
            ARCH_FLAG := "-m32"
        endif
        PLATFORM := $(OS)-$(ISA)$(ARCH)
    endif
endif

ifeq ($(findstring CYGWIN,$(TARGETOS)),CYGWIN)
    OS := windows
    ISA := ia32
endif

# There are three variants for Maxine VE, owing to the 32/64 dom0 variants
# MaxVE: 64 bit dom0, Inspector running in dom0, with libtele referencing 64-bit libguk/libxen*
# MaxVE64H: Inspector running in domU, with 64 bit libtele not referencing libguk/libxen
# MaxVE32T: 32 bit dom0, Inspector agent running in dom0, with 32 bit libtele referencing 32 bit libguk/libxen

ifeq ($(TARGETOS),MaxVE)
    HYP := xen
    OS := maxve
    TELEBITS := 64
    GUK := 1
    ISA := amd64
    ARCH := amd64
endif

ifeq ($(TARGETOS),MaxVE64H)
    HYP := xen
    OS := maxve
    TELEBITS := 64
    GUK := 0
    ISA := amd64
    ARCH := amd64
endif

ifeq ($(TARGETOS),MaxVE32T)
    HYP := xen
    OS := maxve
    TELEBITS := 32
    GUK := 1
    ISA := amd64
    ARCH := amd64
endif

ifndef OS
    $(error unknown OS)
endif

ifndef ISA
    $(error unknown ISA)
endif

ifndef PLATFORM
    PLATFORM := $(OS)-$(ISA)
endif


# identify the JDK version (there are differences in how Maxine is built between JDK 7 and 8)
ifdef JAVA_HOME
	javacmd := $(JAVA_HOME)/bin/java
else
	javacmd := java
endif
jdk_version := $(shell $(javacmd) -version 2>&1 | head -n 1)
ifeq ($(findstring 1.7,$(jdk_version)),1.7)
	JDK := -DJDK7
else
    ifeq ($(findstring 1.8,$(jdk_version)),1.8)
        JDK := -DJDK8
    endif
endif


# Always build libhosted wih gcc when cross-compiling
ifeq ($(TARGET), HOSTED)
	ifeq ($(CROSS_COMPILING), 1)
        CC := gcc
    endif
endif

ifeq ($(OS),darwin)
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = gcc
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g $(DARWIN_GCC_MFLAG) -Wall -Wextra -Werror -Wno-main -Wno-deprecated-declarations -Wno-unused-parameter -fPIC -DDARWIN -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE) $(JDK)
    endif
    C_DEPENDENCIES_FLAGS = -M -DDARWIN -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    LINK_MAIN = $(CC) -g $(DARWIN_GCC_MFLAG) -lc -lm -ldl -framework CoreFoundation -o $(MAIN)
    # The version linker flags below are required by libjava.jnilib which expects the jvm shared
    # library to have a certain version number.
    ifdef JDK
        LINK_LIB = $(CC) -g $(DARWIN_GCC_MFLAG) -dynamiclib -undefined dynamic_lookup \
            -Xlinker -compatibility_version -Xlinker 1.0.0 \
            -Xlinker -rpath -Xlinker $(shell mkdir -p $(PROJECT)/generated/$(OS) && cd $(PROJECT)/generated/$(OS) && /bin/pwd) \
            -Xlinker -current_version -Xlinker 1.0.0 \
            -lc -lm
    else
        LINK_LIB = $(CC) -g $(DARWIN_GCC_MFLAG) -dynamiclib -undefined dynamic_lookup \
            -Xlinker -compatibility_version -Xlinker 1.0.0 \
            -Xlinker -current_version -Xlinker 1.0.0 \
            -lc -lm
    endif
    LIB_PREFIX = lib
    LIB_SUFFIX = .dylib
    JAVA_HOME ?= /Library/Java/JavaVirtualMachines/jdk1.8.0_151.jdk/Contents/Home
endif

ifeq ($(OS),linux)
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = gcc
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g -Wall -Wno-long-long -Werror -Wextra -Wno-main -Wno-unused-parameter -fPIC -D_GNU_SOURCE -D$(ISA) -DLINUX -D$(TARGET) -D$(TARGET_WORD_SIZE) $(JDK) $(OTHER_CFLAGS)
    endif
    C_DEPENDENCIES_FLAGS = -M -DLINUX -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    # The '-rpath' linker option is used so that LD_LIBRARY_PATH does not have to be configured at runtime to
    # find Maxine's version of the libjvm.so library.
    # The '-z execstack' is a workaround that stops the runtime dynamic linker from
    # changing the protection of the main thread's stack (via mprotect) *after* the
    # yellow guard page (for detecting stack overflow) has been mprotected. Without
    # this flag, the main thread's complete stack (including the guard page) is
    # mprotected with PROT_READ, PROT_WRITE, PROT_EXEC when dlopen() is called to
    # open libjava.so.
    LINK_MAIN = $(CC) -z execstack -g -rdynamic -Xlinker -rpath -Xlinker $(shell cd $(PROJECT)/generated/$(OS) && /bin/pwd) -o $(MAIN)
    # Libraries must be specified after the actual source files, so the POSTFIX variable is used for that
    # (Introduced to solve a linking problem on Ubuntu 11.10)
    LINK_MAIN_POSTFIX = -lc -lm -lpthread -ldl
    ifeq ($(ISA),arm)
        LINK_MAIN_POSTFIX += -lstdc++
    endif
    LINK_LIB = $(CC) -g -shared
    LINK_LIB_POSTFIX = -lc -lm -lpthread
    ifeq ($(ISA),arm)
        LINK_LIB_POSTFIX += -lstdc++ -ldl
    endif
    ifeq ($(ISA),amd64)
        LINK_LIB_POSTFIX += -lnuma
    endif
    LIB_PREFIX = lib
    LIB_SUFFIX = .so
endif

ifeq ($(OS),solaris)
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = cc
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g -xc99 -errwarn -errtags -errfmt=error $(KPIC_FLAG) $(ARCH_FLAG) -D$(ISA) -DSOLARIS -D$(TARGET) -D$(TARGET_WORD_SIZE) $(OTHER_CFLAGS) $(JDK)
    endif
    C_DEPENDENCIES_FLAGS = -xM1 -DSOLARIS -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    # The '-R' linker option is used so that LD_LIBRARY_PATH does not have to be configured at runtime to
    # find Maxine's version of the libjvm.so library.
    LINK_MAIN = $(CC) $(ARCH_FLAG) -lc -lthread -ldl -R$(shell cd $(PROJECT)/generated/$(OS) && /bin/pwd) -o $(MAIN)
    LINK_LIB = $(CC) -G $(ARCH_FLAG) -lresolv -lc -lm -ldl -lthread -lrt -lproc
    LIB_PREFIX = lib
    LIB_SUFFIX = .so
endif

ifeq ($(OS),windows)
    # determine predefined macros: touch foo.c; gcc -E -dD foo.c
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = gcc
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g -ansi -Wall -pedantic -Wno-long-long -mno-cygwin -DWINDOWS -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE) $(JDK)
    endif
    C_DEPENDENCIES_FLAGS = -MM -DWINDOWS -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    LINK_MAIN = $(CC) -g -mno-cygwin -Wall -W1,----add-stdcall-alias -ldl
    LINK_LIB = $(CC) -g -shared -mno-cygwin -Wall -W1,----add-stdcall-alias
    LIB_PREFIX =
    LIB_SUFFIX = .dll
endif

ifeq ($(OS),maxve)
    # assume Xen hypervisor
    ifeq ($(TARGET),TELE)
        ifeq ($(TELEBITS),64)
          mf = -m64
        else
          mf = -m32
          tdir = /32bit
        endif
    else
      mf = -m64
    endif
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = gcc
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g -Wall -Wno-format -Wpointer-arith -Winline \
                  $(mf) -mno-red-zone -fpic -fno-reorder-blocks \
                  -fno-asynchronous-unwind-tables -fno-builtin \
                  -DMAXVE -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE) $(JDK)
    endif
    C_DEPENDENCIES_FLAGS = -M -DMAXVE -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    ifeq ($(HOSTOS),Linux)
        CFLAGS += -fno-stack-protector
    endif
    LIB_PREFIX = lib
    LIB_SUFFIX = .so
    LIBA_SUFFIX = .a
    ifeq ($(HOSTOS),Linux)
        AR = ar
    else
        AR = gar
    endif

    LINK_AR = $(AR) r $(LIB_PREFIX)$(LIB)$(LIBA_SUFFIX)
    ifeq ($(GUK),1)
        XG_ROOT = $(XEN_ROOT)/tools/debugger/gdbsx/xg
        ifeq "$(realpath $(XG_ROOT))" ""
            LINK_LIB = $(CC) -shared -lc -lm $(mf)  -L ../../../../../guk/tools/db-front$(tdir) -lguk_db
        else
            LINK_LIB = $(CC) -shared -lc -lm $(mf)  -L ../../../../../guk/tools/db-front$(tdir) -lguk_db $(XG_ROOT)/xg_main.o $(XG_ROOT)/xg_64.o $(XG_ROOT)/xg_32.o
        endif
    else
        LINK_LIB = $(CC) -shared -lc -lm $(mf)
    endif
endif

ifndef JAVA_HOME
    ignore := $(error "Must set JAVA_HOME environment variable to your JDK home directory")
endif


ifeq ($(OS),maxve)
    # no maxve in your typical JAVA_HOME so have to use host
    ifeq ($(HOSTOS),Darwin)
        HOSTOS_LC = darwin
    endif
    ifeq ($(HOSTOS),Linux)
        HOSTOS_LC = linux
    endif
    ifeq ($(HOSTOS),SunOS)
        HOSTOS_LC = solaris
    endif
    JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(HOSTOS_LC)
    JNI_H_PATH = $(wildcard $(JAVA_HOME)/include/jni.h)
    # if we are building TELE need access to Xen debugger header
    ifeq ($(TARGET),TELE)
        ifndef XEN_ROOT
            ignore := $(error "Must set XEN_ROOT environment variable to root of your Xen source tree")
        endif
        CFLAGS += -I $(XEN_ROOT)/tools/debugger/gdbsx/xg
    endif

else
    ifeq ($(OS),darwin)
        ifdef JDK
             JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS)
             JNI_H_PATH = $(wildcard $(JAVA_HOME)/include/jni.h)
        else
            JNI_H_PATH=$(shell ls $(foreach base, /Developer/SDKs/MacOSX10.*.sdk/ ,$(base)System/Library/Frameworks/JavaVM.framework/Versions/*/Headers/jni.h) 2>/dev/null | tail -1)
            ifeq "$(JNI_H_PATH)" ""
                 JNI_H_PATH=$(shell ls $(foreach base, / ,$(base)System/Library/Frameworks/JavaVM.framework/Versions/*/Headers/jni.h) 2>/dev/null | tail -1)
            endif
            JNI_INCLUDES = -I $(dir $(JNI_H_PATH))
        endif
    else
        JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS)
        JNI_H_PATH = $(wildcard $(JAVA_HOME)/include/jni.h)
    endif
endif

ifeq "$(JNI_H_PATH)" ""
    $(error Could not find path to jni.h in $(JAVA_HOME))
endif

C_DEPENDENCIES_FLAGS += $(JNI_INCLUDES) -DJNI_H_PATH=\"$(JNI_H_PATH)\"
CFLAGS += $(JNI_INCLUDES) -DJNI_H_PATH=\"$(JNI_H_PATH)\"
