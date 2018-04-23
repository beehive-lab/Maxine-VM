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
#include "isa.h"
#include "log.h"

void isa_canonicalizeTeleIntegerRegisters(isa_OsTeleIntegerRegisters os, isa_CanonicalIntegerRegisters c) {

#if os_LINUX
#define CANONICALIZE(reg) c->r##reg = (Word) os->regs[reg]
#else
#define CANONICALIZE(reg, ucReg) c_UNIMPLEMENTED()
#endif

    CANONICALIZE(0);
    CANONICALIZE(1);
    CANONICALIZE(2);
    CANONICALIZE(3);
    CANONICALIZE(4);
    CANONICALIZE(5);
    CANONICALIZE(6);
    CANONICALIZE(7);
    CANONICALIZE(8);
    CANONICALIZE(9);
    CANONICALIZE(10);
    CANONICALIZE(11);
    CANONICALIZE(12);
    CANONICALIZE(13);
    CANONICALIZE(14);
    CANONICALIZE(15);
    CANONICALIZE(16);
    CANONICALIZE(17);
    CANONICALIZE(18);
    CANONICALIZE(19);
    CANONICALIZE(20);
    CANONICALIZE(21);
    CANONICALIZE(22);
    CANONICALIZE(23);
    CANONICALIZE(24);
    CANONICALIZE(25);
    CANONICALIZE(26);
    CANONICALIZE(27);
    CANONICALIZE(28);
    CANONICALIZE(29);
    CANONICALIZE(30);

#undef CANONICALIZE
}

void isa_canonicalizeTeleFloatingPointRegisters(isa_OsTeleFloatingPointRegisters os, isa_CanonicalFloatingPointRegisters c) {
    log_println("Aarch64: isa_canonicalizeTeleFloatingPointRegisters is not implemented!");
}

void isa_canonicalizeTeleStateRegisters(isa_OsTeleStateRegisters os, isa_CanonicalStateRegisters c) {
    c->sp = (Word) os->sp;
    c->pc = (Word) os->pc;
    c->pstate = (Word) os->pstate;
}

void isa_printCanonicalIntegerRegisters(isa_CanonicalIntegerRegisters c) {
#define PRINT_REG(reg) log_println(#reg " = %p [%ld]", c->reg, c->reg)

    PRINT_REG(r0);
    PRINT_REG(r1);
    PRINT_REG(r2);
    PRINT_REG(r3);
    PRINT_REG(r4);
    PRINT_REG(r5);
    PRINT_REG(r6);
    PRINT_REG(r7);
    PRINT_REG(r8);
    PRINT_REG(r9);
    PRINT_REG(r10);
    PRINT_REG(r11);
    PRINT_REG(r12);
    PRINT_REG(r13);
    PRINT_REG(r14);
    PRINT_REG(r15);
    PRINT_REG(r16);
    PRINT_REG(r17);
    PRINT_REG(r18);
    PRINT_REG(r19);
    PRINT_REG(r20);
    PRINT_REG(r21);
    PRINT_REG(r22);
    PRINT_REG(r23);
    PRINT_REG(r24);
    PRINT_REG(r25);
    PRINT_REG(r26);
    PRINT_REG(r27);
    PRINT_REG(r28);
    PRINT_REG(r29);
    PRINT_REG(r30);

#undef PRINT_REG
}

void isa_printCanonicalFloatingPointRegisters(isa_CanonicalFloatingPointRegisters c) {
    log_println("Aarch64: isa_printCanonicalFloatingPointRegisters is not implemented!");
}

void isa_printCanonicalStateRegisters(isa_CanonicalStateRegisters canonicalStateRegisters) {
    log_println("sp     = %p [%ld]", canonicalStateRegisters->sp, canonicalStateRegisters->sp);
    log_println("pc     = %p [%ld]", canonicalStateRegisters->pc, canonicalStateRegisters->pc);
    log_println("pstate = %p [%ld]", canonicalStateRegisters->pstate, canonicalStateRegisters->pstate);
}
