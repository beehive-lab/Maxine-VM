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
#define CANONICALIZE(reg) c->x##reg = (Word) os->regs[reg]
#else
#define CANONICALIZE(reg, ucReg) c_UNIMPLEMENTED()
#endif

    // CANONICALIZE(0);
    CANONICALIZE(1);
    // CANONICALIZE(2); this is SP
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
    CANONICALIZE(31);

#undef CANONICALIZE
}

void isa_canonicalizeTeleFloatingPointRegisters(isa_OsTeleFloatingPointRegisters os, isa_CanonicalFloatingPointRegisters c) {
    log_println("Riscv64: isa_canonicalizeTeleFloatingPointRegisters is not implemented!");
}

void isa_canonicalizeTeleStateRegisters(isa_OsTeleStateRegisters os, isa_CanonicalStateRegisters c) {
    c->sp = (Word) os->regs[2];
    c->pc = (Word) os->pc;
    // TODO ask about this. Not sure what it is.
    // c->pstate = (Word) os->pstate; 
}

void isa_printCanonicalIntegerRegisters(isa_CanonicalIntegerRegisters c) {
#define PRINT_REG(reg) log_println(#reg " = %p [%ld]", c->reg, c->reg)

    // PRINT_REG(r0);
    PRINT_REG(x1);
    // PRINT_REG(x2); // This is SP
    PRINT_REG(x3);
    PRINT_REG(x4);
    PRINT_REG(x5);
    PRINT_REG(x6);
    PRINT_REG(x7);
    PRINT_REG(x8);
    PRINT_REG(x9);
    PRINT_REG(x10);
    PRINT_REG(x11);
    PRINT_REG(x12);
    PRINT_REG(x13);
    PRINT_REG(x14);
    PRINT_REG(x15);
    PRINT_REG(x16);
    PRINT_REG(x17);
    PRINT_REG(x18);
    PRINT_REG(x19);
    PRINT_REG(x20);
    PRINT_REG(x21);
    PRINT_REG(x22);
    PRINT_REG(x23);
    PRINT_REG(x24);
    PRINT_REG(x25);
    PRINT_REG(x26);
    PRINT_REG(x27);
    PRINT_REG(x28);
    PRINT_REG(x29);
    PRINT_REG(x30);
    PRINT_REG(x31);

#undef PRINT_REG
}

void isa_printCanonicalFloatingPointRegisters(isa_CanonicalFloatingPointRegisters c) {
    log_println("Riscv64: isa_printCanonicalFloatingPointRegisters is not implemented!");
}

void isa_printCanonicalStateRegisters(isa_CanonicalStateRegisters canonicalStateRegisters) {
    log_println("sp     = %p [%ld]", canonicalStateRegisters->sp, canonicalStateRegisters->sp);
    log_println("pc     = %p [%ld]", canonicalStateRegisters->pc, canonicalStateRegisters->pc);
    // log_println("pstate = %p [%ld]", canonicalStateRegisters->pstate, canonicalStateRegisters->pstate);
}
