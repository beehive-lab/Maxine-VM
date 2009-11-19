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

extern Address virtualMemory_allocateAtFixedAddress(Address address, Size size, int type);

extern Address virtualMemory_allocate(Size size, int type);
extern Address virtualMemory_allocateIn31BitSpace(Size size, int type);
extern Address virtualMemory_allocateNoSwap(Size size, int type);
extern Address virtualMemory_deallocate(Address start, Size size, int type);

extern unsigned int virtualMemory_getPageSize(void);

extern Address virtualMemory_pageAlign(Address address);

extern void virtualMemory_protectPages(Address address, int count);
extern void virtualMemory_unprotectPages(Address address, int count);
#endif /*__virtualMemory_h__*/
