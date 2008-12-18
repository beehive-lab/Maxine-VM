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
#ifndef __amd64_h__
#define __amd64_h__ 1

#include "word.h"

#if os_DARWIN
#   include <sys/ucontext.h>
    typedef _STRUCT_X86_THREAD_STATE64 *amd64_OsSignalIntegerRegisters;
    typedef _STRUCT_X86_FLOAT_STATE64 *amd64_OsSignalFloatingPointRegisters;
    typedef _STRUCT_X86_THREAD_STATE64 *amd64_OsTeleIntegerRegisters;
    typedef _STRUCT_X86_THREAD_STATE64 *amd64_OsTeleStateRegisters;
    typedef _STRUCT_X86_FLOAT_STATE64 *amd64_OsTeleFloatingPointRegisters;
#elif os_LINUX
#   include <sys/ucontext.h>
#   include <sys/user.h>
    typedef greg_t *amd64_OsSignalIntegerRegisters;
    typedef fpregset_t amd64_OsSignalFloatingPointRegisters;
    typedef struct user_regs_struct *amd64_OsTeleIntegerRegisters;
    typedef struct user_regs_struct *amd64_OsTeleFloatingPointRegisters;
    typedef struct user_regs_struct *amd64_OsTeleStateRegisters;
#elif os_SOLARIS
#   include <sys/procfs.h>
	typedef greg_t *amd64_OsSignalIntegerRegisters;
	typedef fpregset_t *amd64_OsSignalFloatingPointRegisters;
	typedef prgreg_t *amd64_OsTeleIntegerRegisters;
	typedef prfpregset_t *amd64_OsTeleFloatingPointRegisters;
	typedef prgreg_t *amd64_OsTeleStateRegisters;
#elif os_GUESTVMXEN
#   include <guestvmXen_db.h>
#   include <guestvmXen.h>
    typedef struct fault_regs* amd64_OsSignalIntegerRegisters;
    typedef struct fault_regs* amd64_OsSignalFloatingPointRegisters;
    typedef struct db_regs* amd64_OsTeleIntegerRegisters;
    typedef struct db_regs* amd64_OsTeleStateRegisters;
    typedef struct db_regs* amd64_OsTeleFloatingPointRegisters;
#else
#   error
#endif

typedef struct amd64_CanonicalIntegerRegisters {
	Word rax;
	Word rcx;
	Word rdx;
	Word rbx;
	Word rsp;
	Word rbp;
	Word rsi;
	Word rdi;
	Word r8;
	Word r9;
	Word r10;
	Word r11;
	Word r12;
	Word r13;
	Word r14;
	Word r15;
} amd64_CanonicalIntegerRegistersAggregate, *amd64_CanonicalIntegerRegisters;

extern void amd64_decanonicalizeSignalIntegerRegisters(amd64_CanonicalIntegerRegisters c, amd64_OsSignalIntegerRegisters os);
extern void amd64_canonicalizeSignalIntegerRegisters(amd64_OsSignalIntegerRegisters osSignalIntegerRegisters, amd64_CanonicalIntegerRegisters canonicalIntegerRegisters);
extern void amd64_canonicalizeTeleIntegerRegisters(amd64_OsTeleIntegerRegisters osTeleIntegerRegisters, amd64_CanonicalIntegerRegisters canonicalIntegerRegisters);
extern void amd64_printCanonicalIntegerRegisters(amd64_CanonicalIntegerRegisters canonicalIntegerRegisters);

typedef struct amd64_CanonicalFloatingPointRegisters {
	Word xmm0;
	Word xmm1;
	Word xmm2;
	Word xmm3;
	Word xmm4;
	Word xmm5;
	Word xmm6;
	Word xmm7;
	Word xmm8;
	Word xmm9;
	Word xmm10;
	Word xmm11;
	Word xmm12;
	Word xmm13;
	Word xmm14;
	Word xmm15;
} amd64_CanonicalFloatingPointRegistersAggregate, *amd64_CanonicalFloatingPointRegisters;

extern void amd64_canonicalizeSignalFloatingPointRegisters(amd64_OsSignalFloatingPointRegisters osSignalFloatingPointRegisters, amd64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);
extern void amd64_canonicalizeTeleFloatingPointRegisters(amd64_OsTeleFloatingPointRegisters osTeleFloatingPointRegisters, amd64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);
extern void amd64_printCanonicalFloatingPointRegisters(amd64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

typedef struct amd64_CanonicalStateRegisters {
	Word rip;
	Word flags;
} amd64_CanonicalStateRegistersAggregate, *amd64_CanonicalStateRegisters;

extern void amd64_canonicalizeTeleStateRegisters(amd64_OsTeleStateRegisters osTeleStateRegisters, amd64_CanonicalStateRegisters canonicalStateRegisters);

#endif /*__amd64_h__*/
