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
#ifndef __ia32_h__
#define __ia32_h__ 1

#include "word.h"

#if os_DARWIN
#	include<mach/thread_status.h> 
	typedef thread_state_t ia32_OsSignalIntegerRegisters;
	typedef thread_state_t ia32_OsTeleStateRegisters;
	typedef thread_state_t ia32_OsTeleIntegerRegisters;
#else
/* So far only Darwin is handled on this arch */
#	error
#endif

typedef struct ia32_CanonicalIntegerRegisters {
    Word    eax;
    Word    ebx;
    Word    ecx;
    Word    edx;
    Word    edi;
    Word    esi;
    Word    ebp;
    Word    esp;
} ia32_CanonicalIntegerRegistersAggregate, *ia32_CanonicalIntegerRegisters;

extern void ia32_decanonicalizeSignalIntegerRegisters(ia32_CanonicalIntegerRegisters c, ia32_OsSignalIntegerRegisters os);
extern void ia32_canonicalizeSignalIntegerRegisters(ia32_OsSignalIntegerRegisters osSignalIntegerRegisters, ia32_CanonicalIntegerRegisters canonicalIntegerRegisters);
extern void ia32_canonicalizeTeleIntegerRegisters(ia32_OsTeleIntegerRegisters osTeleIntegerRegisters, ia32_CanonicalIntegerRegisters canonicalIntegerRegisters);

typedef struct ia32_CanonicalStateRegisters {
	Word eip;
	Word flags;
} ia32_CanonicalStateRegistersAggregate, *ia32_CanonicalStateRegisters;

extern void ia32_canonicalizeStateRegisters(ia32_OsTeleStateRegisters osTeleStateRegisters, ia32_CanonicalStateRegisters canonicalStateRegisters);

#endif /*__ia32_h__*/
