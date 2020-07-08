/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
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
#ifndef __aarch64_h__
#define __aarch64_h__ 1

#include "word.h"

#if os_LINUX
#   include <sys/ucontext.h>
#   include <sys/user.h>
typedef struct user_regs_struct *aarch64_OsTeleIntegerRegisters;
typedef struct user_fpsimd_struct *aarch64_OsTeleFloatingPointRegisters;
typedef struct user_regs_struct *aarch64_OsTeleStateRegisters;
typedef struct {
    Word low;
    Word high;
}XMMRegister;
#else
#   error
#endif
typedef struct aarch64_CanonicalIntegerRegisters {
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
    Word r16;
    Word r17;
    Word r18;
    Word r19;
    Word r20;
    Word r21;
    Word r22;
    Word r23;
    Word r24;
    Word r25;
    Word r26;
    Word r27;
    Word r28;
    Word r29;
    Word r30;
} aarch64_CanonicalIntegerRegistersAggregate, *aarch64_CanonicalIntegerRegisters;
typedef struct aarch64_CanonicalFloatingPointRegisters {
    Word d0;
    Word d1;
    Word d2;
    Word d3;
    Word d4;
    Word d5;
    Word d6;
    Word d7;
    Word d8;
    Word d9;
    Word d10;
    Word d11;
    Word d12;
    Word d13;
    Word d14;
    Word d15;
    Word d16;
    Word d17;
    Word d18;
    Word d19;
    Word d20;
    Word d21;
    Word d22;
    Word d23;
    Word d24;
    Word d25;
    Word d26;
    Word d27;
    Word d28;
    Word d29;
    Word d30;
    Word d31;
} aarch64_CanonicalFloatingRegistersAggregate, *aarch64_CanonicalFloatingPointRegisters;

typedef struct aarch64_CanonicalStateRegisters {
    Word sp;
    union {
        Word pc;
        Word rip;
    };
    Word pstate;
} aarch64_CanonicalStateRegistersAggregate, *aarch64_CanonicalStateRegisters;

extern void aarch64_canonicalizeTeleIntegerRegisters(aarch64_OsTeleIntegerRegisters, aarch64_CanonicalIntegerRegisters);

extern void aarch64_canonicalizeTeleFloatingPointRegisters(aarch64_OsTeleFloatingPointRegisters, aarch64_CanonicalFloatingPointRegisters);

extern void aarch64_canonicalizeTeleStateRegisters(aarch64_OsTeleStateRegisters osTeleStateRegisters, aarch64_CanonicalStateRegisters canonicalStateRegisters);

extern void aarch64_printCanonicalIntegerRegisters(aarch64_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void aarch64_printCanonicalFloatingPointRegisters(aarch64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void aarch64_printCanonicalStateRegisters(aarch64_CanonicalStateRegisters canonicalStateRegisters);
#endif /*__aarch64__*/
