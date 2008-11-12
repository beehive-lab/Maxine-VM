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
#include "debug.h"

long total_memory = -1;
long max_memory = -1;
long free_memory = -1;

Address nativeAllocate(Size size)
{
    return (Address) malloc((size_t) size);
}

Address nativeReallocate(Address pointer, Size size)
{
	if (pointer == 0) {
		return (Address) malloc((size_t) size);
	}
    return (Address) realloc((void *) pointer, (size_t) size);
}

jint nativeDeallocate(Address pointer)
{
    free((void *) pointer);
    return 0;
}

void native_writeMaxMemory(long mem)
{
	max_memory = mem;
}

void native_writeTotalMemory(long mem)
{
	total_memory = mem;
}

void native_writeFreeMemory(long mem)
{
	free_memory = mem;
}

jboolean nativeAllocateMemoryAtFixedAddress(Address pointer, Size size){
	if( virtualMemory_allocateAtFixedAddress(pointer, size) == 0){
		perror("nativeAllocateMemoryAtFixedAddress: could not native allocate bytes of memory at fixed address ");
		exit(-1);
	}
	else {
		return true;
	}
}

