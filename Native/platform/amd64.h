/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __amd64_h__
#define __amd64_h__ 1

#include "word.h"

#if os_DARWIN
#   include <sys/ucontext.h>
    typedef _STRUCT_X86_THREAD_STATE64 *amd64_OsTeleIntegerRegisters;
    typedef _STRUCT_X86_THREAD_STATE64 *amd64_OsTeleStateRegisters;
    typedef _STRUCT_X86_FLOAT_STATE64 *amd64_OsTeleFloatingPointRegisters;
#elif os_LINUX
#   include <sys/ucontext.h>
#   include <sys/user.h>
    typedef struct user_regs_struct *amd64_OsTeleIntegerRegisters;
    typedef struct user_fpregs_struct *amd64_OsTeleFloatingPointRegisters;
    typedef struct user_regs_struct *amd64_OsTeleStateRegisters;
    typedef struct {
        Word low;
        Word high;
    } XMMRegister;
#elif os_SOLARIS
#   include <sys/procfs.h>
	typedef prgreg_t *amd64_OsTeleIntegerRegisters;
	typedef prfpregset_t *amd64_OsTeleFloatingPointRegisters;
	typedef prgreg_t *amd64_OsTeleStateRegisters;
#elif os_MAXVE
#   include <maxve_db.h>
#   include <maxve.h>
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

typedef struct amd64_CanonicalStateRegisters {
	Word rip;
	Word flags;
} amd64_CanonicalStateRegistersAggregate, *amd64_CanonicalStateRegisters;

extern void amd64_canonicalizeTeleIntegerRegisters(amd64_OsTeleIntegerRegisters osTeleIntegerRegisters, amd64_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void amd64_canonicalizeTeleFloatingPointRegisters(amd64_OsTeleFloatingPointRegisters osTeleFloatingPointRegisters, amd64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void amd64_canonicalizeTeleStateRegisters(amd64_OsTeleStateRegisters osTeleStateRegisters, amd64_CanonicalStateRegisters canonicalStateRegisters);

extern void amd64_printCanonicalIntegerRegisters(amd64_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void amd64_printCanonicalFloatingPointRegisters(amd64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void amd64_printCanonicalStateRegisters(amd64_CanonicalStateRegisters canonicalStateRegisters);

#endif /*__amd64_h__*/
