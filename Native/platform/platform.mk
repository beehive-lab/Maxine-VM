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

ifeq ($(LIB), prototype)
    TARGET := PROTOTYPE
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

ifeq ($(TARGETOS),GuestVM)
    HYP := xen
    OS := guestvm
    DOM0 := 64
    ISA := amd64
    ARCH := amd64
endif

ifeq ($(TARGETOS),GuestVM32)
    HYP := xen
    OS := guestvm
    DOM0 := 32
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
        CFLAGS = -g $(DARWIN_GCC_MFLAG) -Wall -Wextra -Werror -Wno-main -Wno-unused-parameter -fPIC -DDARWIN -D$(ISA) -D$(TARGET)
    endif
    C_DEPENDENCIES_FLAGS = -M -DDARWIN -D$(ISA) -D$(TARGET)
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
        CFLAGS = -g -Wall -Wno-long-long -Werror -Wextra -Wno-main -Wno-unused-parameter -fPIC -D_GNU_SOURCE -D$(ISA) -DLINUX -D$(TARGET)
    endif
    C_DEPENDENCIES_FLAGS = -M -DLINUX -D$(ISA) -D$(TARGET)
    # The -rpath option is used so that LD_LIBRARY_PATH does not have to be configured at runtime to
    # find Maxine's version of the libjvm.so library. 
    LINK_MAIN = $(CC) -g -lc -lm -lpthread -ldl -rdynamic -Xlinker -rpath -Xlinker $(shell cd $(PROJECT)/generated/$(OS) && /bin/pwd) -o $(MAIN)
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
        CFLAGS = -g -xc99 -errwarn -errtags -errfmt=error $(KPIC_FLAG) $(ARCH_FLAG) -D$(ISA) -DSOLARIS -D$(TARGET) $(OTHER_CFLAGS)
    endif
    C_DEPENDENCIES_FLAGS = -xM1 -DSOLARIS -D$(ISA) -D$(TARGET) 
    LINK_MAIN = $(CC) $(ARCH_FLAG) -lc -lthread -ldl -o $(MAIN)
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
        CFLAGS = -g -ansi -Wall -pedantic -Wno-long-long -mno-cygwin -DWINDOWS -D$(ISA) -D$(TARGET)
    endif   
    C_DEPENDENCIES_FLAGS = -MM -DWINDOWS -D$(ISA) -D$(TARGET)
    LINK_MAIN = $(CC) -g -mno-cygwin -Wall -W1,----add-stdcall-alias -ldl
    LINK_LIB = $(CC) -g -shared -mno-cygwin -Wall -W1,----add-stdcall-alias
    LIB_PREFIX =
    LIB_SUFFIX = .dll
endif

ifeq ($(OS),guestvm)
    # assume Xen hypervisor
    ifeq ($(DOM0),64)
      mf = -m64
    else
      mf = -m32
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
                  -DGUESTVMXEN -D$(ISA) -D$(TARGET)
    endif
    C_DEPENDENCIES_FLAGS = -M -DGUESTVMXEN -D$(ISA) -D$(TARGET)
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
#    LINK_LIB = $(CC) -shared -lc -lm -m64  -lguk_db
     LINK_LIB = $(CC) -shared -lc -lm $(mf)
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
else
    JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS)
endif

C_DEPENDENCIES_FLAGS += $(JNI_INCLUDES)
CFLAGS += $(JNI_INCLUDES)
