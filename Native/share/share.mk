
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

%.d: %.c
	$(CC) $(C_DEPENDENCIES_FLAGS) $(CPPFLAGS) $< | sed 's/$*\.o/& $@/g' > $@

DEPENDENCIES = $(SOURCES:%.c=%.d)

-include $(DEPENDENCIES)

OBJECTS = $(SOURCES:%.c=%.o)

PATHS = $(SOURCE_DIRS:%=$(PROJECT)/%)
vpath %.c  $(PATHS)

CPPFLAGS = $(PATHS:%=-I %)

LIBRARY = $(LIB_PREFIX)$(LIB)$(LIB_SUFFIX)

ifneq ($(OS),guestvm)

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
