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

%.d: %.c
	$(CC) $(C_DEPENDENCIES_FLAGS) $(CPPFLAGS) $< | sed 's/$*\.o/& $@/g' > $@

DEPENDENCIES = $(SOURCES:%.c=%.d)

-include $(DEPENDENCIES)

OBJECTS = $(SOURCES:%.c=%.o)

PATHS = $(SOURCE_DIRS:%=$(PROJECT)/%)
vpath %.c  $(PATHS)

CPPFLAGS = $(PATHS:%=-I %)

LIBRARY = $(LIB_PREFIX)$(LIB)$(LIB_SUFFIX)

ifneq ($(OS),maxve)

$(LIBRARY) : $(OBJECTS)
	$(LINK_LIB) $(OBJECTS) -o $(LIBRARY)
	mkdir -p $(PROJECT)/generated/$(OS)
	cp -f $(LIBRARY) $(PROJECT)/generated/$(OS)

$(MAIN) : $(OBJECTS)
	$(LINK_MAIN) $(OBJECTS)
else
#
# On Xen we only care about two things, the image and the inspector library (tele)
# The former is built as an archive file (.a) and the latter as a .so.
# The tele library can be built for 32 bit execution

ifneq ($(TARGET), SUBSTRATE)
	ifeq ($(TARGET), TELE)
	    ifeq ($(TELEBITS), 32)
	      OST := $(OS)_32
	    else
	      OST := $(OS)   
	    endif
	endif

$(LIBRARY) : $(OBJECTS)
	$(LINK_LIB) $(OBJECTS) -o $(LIBRARY)
	mkdir -p $(PROJECT)/generated/$(OST)
	cp -f $(LIBRARY) $(PROJECT)/generated/$(OST)
else
LIBRARY = $(LIB_PREFIX)$(LIB)$(LIBA_SUFFIX)
OBJECTS += $(PROJECT)/generated/$(OS)/maxine.vm.0.o

$(LIBRARY) : $(OBJECTS)
	$(LINK_AR) $(OBJECTS)
	mkdir -p $(PROJECT)/generated/$(OS)
	cp -f $(LIBRARY) $(PROJECT)/generated/$(OS)
endif
endif
