/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
#ifndef __arm_h__
#define __arm_h__ 1

#include "word.h"
#ifndef __arm__
#define __arm__
#endif
#if os_DARWIN
#   include <sys/ucontext.h>
typedef _STRUCT_X86_THREAD_STATE64 *arm_OsTeleIntegerRegisters;
typedef _STRUCT_X86_THREAD_STATE64 *arm_OsTeleStateRegisters;
typedef _STRUCT_X86_FLOAT_STATE64 *arm_OsTeleFloatingPointRegisters;
#elif os_LINUX
#   include <sys/ucontext.h>
#   include <sys/user.h>
typedef struct user_regs *arm_OsTeleIntegerRegisters;
typedef struct user_fpregs *arm_OsTeleFloatingPointRegisters;
typedef struct user_regs *arm_OsTeleStateRegisters;
typedef struct {
    Word low;
    Word high;
}XMMRegister;
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
typedef struct arm_CanonicalIntegerRegisters {
    Word r0;
    Word r1;
    Word r2;
    Word r3;
    Word r4;
    Word r5;
    Word r6;
    Word r7;
    Word r8;
    Word r9;
    Word r10;
    Word r11;
    Word r12;
    Word r13;
    Word r14;
    Word r15;
} arm_CanonicalIntegerRegistersAggregate, *arm_CanonicalIntegerRegisters;
typedef struct arm_CanonicalFloatingPointRegisters {
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
} arm_CanonicalFloatingRegistersAggregate, *arm_CanonicalFloatingPointRegisters;

typedef struct arm_CanonicalStateRegisters {
    Word rip;
    Word flags;
} arm_CanonicalStateRegistersAggregate, *arm_CanonicalStateRegisters;

#ifdef __arm__
extern void arm_canonicalizeTeleIntegerRegisters(arm_OsTeleIntegerRegisters, arm_CanonicalIntegerRegisters);

extern void arm_canonicalizeTeleFloatingPointRegisters(arm_OsTeleFloatingPointRegisters, arm_CanonicalFloatingPointRegisters);

extern void arm_canonicalizeTeleStateRegisters(arm_OsTeleStateRegisters osTeleStateRegisters, arm_CanonicalStateRegisters canonicalStateRegisters);

extern void arm_printCanonicalIntegerRegisters(arm_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void arm_printCanonicalFloatingPointRegisters(arm_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void arm_printCanonicalStateRegisters(arm_CanonicalStateRegisters canonicalStateRegisters);
#endif
#endif /*__arm__*/
