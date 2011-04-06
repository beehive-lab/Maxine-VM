/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __virtualMemory_h__
#define __virtualMemory_h__ 1

#include "word.h"
#include "jni.h"

#define HEAP_VM 0
#define STACK_VM 1
#define CODE_VM 2
#define DATA_VM 3

#define ALLOC_FAILED ((Address) 0)  // return value for failed allocations

extern Address virtualMemory_mapFileIn31BitSpace(jint size, jint fd, Size offset);

extern Address virtualMemory_mapFileAtFixedAddress(Address address, Size size, jint fd, Size offset);

extern boolean virtualMemory_allocateAtFixedAddress(Address address, Size size, int type);

extern Address virtualMemory_allocate(Size size, int type);
extern Address virtualMemory_allocateIn31BitSpace(Size size, int type);
extern Address virtualMemory_allocatePrivateAnon(Address address, Size size, jboolean reserveSwap, jboolean protNone, int type);
extern Address virtualMemory_deallocate(Address start, Size size, int type);

extern unsigned int virtualMemory_getPageSize(void);

extern Address virtualMemory_pageAlign(Address address);

extern void virtualMemory_protectPages(Address address, int count);
extern void virtualMemory_unprotectPages(Address address, int count);
#endif /*__virtualMemory_h__*/
