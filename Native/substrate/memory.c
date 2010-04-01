/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/**
 * @author Bernd Mathiske
 */
#include "os.h"

#include <sys/types.h>
#include <sys/mman.h>
#include <stdlib.h>
#include <string.h>

#include "word.h"
#include "jni.h"
#include "memory.h"
#include "virtualMemory.h"
#include "log.h"

Address memory_allocate(Size size)
{
    Address mem = (Address) calloc(1, (size_t) size);
    if (mem % sizeof(void *)) {
        log_println("MEMORY ALLOCATED NOT WORD-ALIGNED (size:%d at address:%x, void* size: %d)", size, mem, sizeof(void *));
    }
    return mem;
}

Address memory_reallocate(Address pointer, Size size)
{
    Address mem;
	if (pointer == 0) {
		mem = (Address) calloc(1, (size_t) size);
	} else {
	    mem = (Address) realloc((void *) pointer, (size_t) size);
	}
	//log_println("MEMORY ALLOCATED of size:%d at address:%x", size, mem);
    return mem;
}

jint memory_deallocate(Address pointer)
{
    free((void *) pointer);
    //log_println("MEMORY FREED at address: %x", pointer);
    return 0;
}
