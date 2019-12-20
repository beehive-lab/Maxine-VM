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
#ifndef __riscv64_h__
#define __riscv64_h__ 1

#include "word.h"

#if os_LINUX
#   include <sys/ucontext.h>
#   include <sys/user.h>
// For some reason sys/user.h for RISC-V lacks definitions of user_regs_struct and user_fpsimd_struct. We have to manually define those in our own userriscv64.h
#   include "userriscv64.h"
typedef struct user_regs_struct *riscv64_OsTeleIntegerRegisters;
typedef struct user_fpsimd_struct *riscv64_OsTeleFloatingPointRegisters;
typedef struct user_regs_struct *riscv64_OsTeleStateRegisters;
typedef struct {
    Word low;
    Word high;
}XMMRegister;
#else
#   error
#endif
typedef struct riscv64_CanonicalIntegerRegisters {
    Word x1;
    Word x2;
    Word x3;
    Word x4;
    Word x5;
    Word x6;
    Word x7;
    Word x8;
    Word x9;
    Word x10;
    Word x11;
    Word x12;
    Word x13;
    Word x14;
    Word x15;
    Word x16;
    Word x17;
    Word x18;
    Word x19;
    Word x20;
    Word x21;
    Word x22;
    Word x23;
    Word x24;
    Word x25;
    Word x26;
    Word x27;
    Word x28;
    Word x29;
    Word x30;
    Word x31;
} riscv64_CanonicalIntegerRegistersAggregate, *riscv64_CanonicalIntegerRegisters;
typedef struct riscv64_CanonicalFloatingPointRegisters {
    Word f0;
    Word f1;
    Word f2;
    Word f3;
    Word f4;
    Word f5;
    Word f6;
    Word f7;
    Word f8;
    Word f9;
    Word f10;
    Word f11;
    Word f12;
    Word f13;
    Word f14;
    Word f15;
    Word f16;
    Word f17;
    Word f18;
    Word f19;
    Word f20;
    Word f21;
    Word f22;
    Word f23;
    Word f24;
    Word f25;
    Word f26;
    Word f27;
    Word f28;
    Word f29;
    Word f30;
    Word f31;
} riscv64_CanonicalFloatingRegistersAggregate, *riscv64_CanonicalFloatingPointRegisters;

typedef struct riscv64_CanonicalStateRegisters {
    Word sp;
    union {
        Word pc;
        Word rip;
    };
    // Word pstate;
} riscv64_CanonicalStateRegistersAggregate, *riscv64_CanonicalStateRegisters;

extern void riscv64_canonicalizeTeleIntegerRegisters(riscv64_OsTeleIntegerRegisters, riscv64_CanonicalIntegerRegisters);

extern void riscv64_canonicalizeTeleFloatingPointRegisters(riscv64_OsTeleFloatingPointRegisters, riscv64_CanonicalFloatingPointRegisters);

extern void riscv64_canonicalizeTeleStateRegisters(riscv64_OsTeleStateRegisters osTeleStateRegisters, riscv64_CanonicalStateRegisters canonicalStateRegisters);

extern void riscv64_printCanonicalIntegerRegisters(riscv64_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void riscv64_printCanonicalFloatingPointRegisters(riscv64_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void riscv64_printCanonicalStateRegisters(riscv64_CanonicalStateRegisters canonicalStateRegisters);
#endif /*__riscv64__*/
