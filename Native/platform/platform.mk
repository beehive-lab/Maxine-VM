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
# (required for GuestVM/Xen when building tele/inspector)
TARGETOS ?= $(shell uname -s)

ifeq ($(TARGETOS),Darwin)
    OS := darwin
    DARWIN_GCC_MFLAG :=
    a := $(shell uname -p)
    ifeq ($a,i386)
        mach := $(shell ls /usr/include/mach/x86_64)
        ifneq ($(mach), )
    	    DARWIN_GCC_MFLAG := -m64
            ISA := amd64
        else
            ISA := ia32
        endif
    else
       ifeq ($a,powerpc)
           ISA := power
       else
           ISA := $a
       endif
    endif
endif

ifeq ($(TARGETOS),Linux)
    OS := linux
    
    a := $(shell uname -m)
    ifeq ($a,x86_64)
        ISA := amd64
    else 
        ifeq ($a, x86)
            ISA := ia32
        else
            ISA := $a
        endif
    endif
endif

ifeq ($(TARGETOS),SunOS)
    OS := solaris
    OTHER_CFLAGS := -KPIC  -DLP64
    a := $(shell isainfo -n)
    ifeq ($a,amd64)
        ISA := amd64
        ARCH := amd64
        ARCH_FLAG := "-m64"
        OTHER_CFLAGS := -Kpic
    else
        ifeq ($a,sparcv9)
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

# There are three variants for Guest VM, owing to the 32/64 dom0 variants
# GuestVM: 64 bit dom0, Inspector running in dom0, with libtele referencing 64-bit libguk/libxen*
# GuestVM64H: Inspector running in domU, with 64 bit libtele not referencing libguk/libxen
# GuestVM32T: 32 bit dom0, Inspector agent running in dom0, with 32 bit libtele referencing 32 bit libguk/libxen

ifeq ($(TARGETOS),GuestVM)
    HYP := xen
    OS := guestvm
    TELEBITS := 64
    GUK := 1
    ISA := amd64
    ARCH := amd64
endif

ifeq ($(TARGETOS),GuestVM64H)
    HYP := xen
    OS := guestvm
    TELEBITS := 64
    GUK := 0
    ISA := amd64
    ARCH := amd64
endif

ifeq ($(TARGETOS),GuestVM32T)
    HYP := xen
    OS := guestvm
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


ifeq ($(OS),darwin)
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = gcc 
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g $(DARWIN_GCC_MFLAG) -Wall -Wextra -Werror -Wno-main -Wno-unused-parameter -fPIC -DDARWIN -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    endif
    C_DEPENDENCIES_FLAGS = -M -DDARWIN -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    LINK_MAIN = $(CC) -g $(DARWIN_GCC_MFLAG) -lc -lm -ldl -framework CoreFoundation -o $(MAIN)
    # The version linker flag below ensure are required by the modified version of
    # libjava.jnilib that is put into the $(PROJECT)/generated/$(OS) directory
    # by running $(PROJECT)/../bin/mod-macosx-javalib.sh. This library expects the jvm shared
    # library to have a certain version number.
    LINK_LIB = $(CC) -g $(DARWIN_GCC_MFLAG) -dynamiclib -undefined dynamic_lookup \
        -Xlinker -compatibility_version -Xlinker 1.0.0 \
        -Xlinker -current_version -Xlinker 1.0.0 \
        -lc -lm
    LIB_PREFIX = lib
    LIB_SUFFIX = .dylib
    JAVA_HOME ?= /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home
endif

ifeq ($(OS),linux)
    ifneq "$(findstring def, $(origin CC))" ""
        # origin of CC is either undefined or default, so set it here
        CC = gcc
    endif
    ifneq "$(findstring def, $(origin CFLAGS))" ""
        # origin of CFLAGS is either undefined or default, so set it here
        CFLAGS = -g -Wall -Wno-long-long -Werror -Wextra -Wno-main -Wno-unused-parameter -fPIC -D_GNU_SOURCE -D$(ISA) -DLINUX -D$(TARGET) -D$(TARGET_WORD_SIZE)
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
    LINK_MAIN = $(CC) -z execstack -g -lc -lm -lpthread -ldl -rdynamic -Xlinker -rpath -Xlinker $(shell cd $(PROJECT)/generated/$(OS) && /bin/pwd) -o $(MAIN)
    LINK_LIB = $(CC) -g -shared -lc -lm
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
        CFLAGS = -g -xc99 -errwarn -errtags -errfmt=error $(KPIC_FLAG) $(ARCH_FLAG) -D$(ISA) -DSOLARIS -D$(TARGET) -D$(TARGET_WORD_SIZE) $(OTHER_CFLAGS)
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
        CFLAGS = -g -ansi -Wall -pedantic -Wno-long-long -mno-cygwin -DWINDOWS -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    endif   
    C_DEPENDENCIES_FLAGS = -MM -DWINDOWS -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    LINK_MAIN = $(CC) -g -mno-cygwin -Wall -W1,----add-stdcall-alias -ldl
    LINK_LIB = $(CC) -g -shared -mno-cygwin -Wall -W1,----add-stdcall-alias
    LIB_PREFIX =
    LIB_SUFFIX = .dll
endif

ifeq ($(OS),guestvm)
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
                  -DGUESTVMXEN -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
    endif
    C_DEPENDENCIES_FLAGS = -M -DGUESTVMXEN -D$(ISA) -D$(TARGET) -D$(TARGET_WORD_SIZE)
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


ifeq ($(OS),guestvm)
    # no guestvm in your typical JAVA_HOME so have to use host 
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
    # if we are building TELE need access to Xen debugger header
    ifeq ($(TARGET),TELE) 
        ifndef XEN_ROOT
            ignore := $(error "Must set XEN_ROOT environment variable to root of your Xen source tree")
        endif
        CFLAGS += -I $(XEN_ROOT)/tools/debugger/gdbsx/xg
    endif
    
else
    ifeq ($(OS),darwin)
        JNI_H_PATH=$(shell ls $(foreach base,/Developer/SDKs/MacOSX10.*.sdk/ /,$(base)System/Library/Frameworks/JavaVM.framework/Versions/1.6*/Headers/jni.h) 2>/dev/null | tail -1)
        JNI_INCLUDES = -I $(dir $(JNI_H_PATH))
    else
        JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS)
        JNI_H_PATH = $(wildcard $(JAVA_HOME)/include/jni.h)
    endif
endif

ifeq "$(JNI_H_PATH)" ""
    $(error Could not find path to jni.h)
endif

C_DEPENDENCIES_FLAGS += $(JNI_INCLUDES) -DJNI_H_PATH=\"$(JNI_H_PATH)\"
CFLAGS += $(JNI_INCLUDES) -DJNI_H_PATH=\"$(JNI_H_PATH)\"
