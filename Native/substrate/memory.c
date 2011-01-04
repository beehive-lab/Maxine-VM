/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
