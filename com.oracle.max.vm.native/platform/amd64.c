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
#include "isa.h"
#include "log.h"

void isa_canonicalizeTeleIntegerRegisters(isa_OsTeleIntegerRegisters os, isa_CanonicalIntegerRegisters c) {

#if os_DARWIN
#define CANONICALIZE(reg, ucReg) c->reg = (Word) os->__##reg
#elif os_LINUX || os_MAXVE
#define CANONICALIZE(reg, ucReg) c->reg = (Word) os->reg
#elif os_SOLARIS
#define CANONICALIZE(reg, ucReg) c->reg = (Word) os[REG_##ucReg]
#else
#define CANONICALIZE(reg, ucReg) c_UNIMPLEMENTED()
#endif

    CANONICALIZE(rax, RAX);
    CANONICALIZE(rcx, RCX);
    CANONICALIZE(rdx, RDX);
    CANONICALIZE(rbx, RBX);
    CANONICALIZE(rsp, RSP);
    CANONICALIZE(rbp, RBP);
    CANONICALIZE(rsi, RSI);
    CANONICALIZE(rdi, RDI);
    CANONICALIZE(r8, R8);
    CANONICALIZE(r9, R9);
    CANONICALIZE(r10, R10);
    CANONICALIZE(r11, R11);
    CANONICALIZE(r12, R12);
    CANONICALIZE(r13, R13);
    CANONICALIZE(r14, R14);
    CANONICALIZE(r15, R15);

#undef CANONICALIZE
}

void isa_canonicalizeTeleFloatingPointRegisters(isa_OsTeleFloatingPointRegisters os, isa_CanonicalFloatingPointRegisters c) {
#if os_DARWIN
#define CANONICALIZE(reg) c->xmm##reg = (*((Word *) (&os->__fpu_xmm##reg)))
#elif os_LINUX
#define CANONICALIZE(reg) c->xmm##reg = (Word) ((XMMRegister *) &os->xmm_space)[reg].low
#elif os_SOLARIS
#define CANONICALIZE(reg) c->xmm##reg = (Word) *((Word *) &os->fp_reg_set.fpchip_state.xmm[reg])
#elif os_MAXVE
#define CANONICALIZE(reg) c->xmm##reg = (Word) os->xmm##reg
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

#undef CANONICALIZE
}

void isa_canonicalizeTeleStateRegisters(isa_OsTeleStateRegisters os, isa_CanonicalStateRegisters c) {
#if os_DARWIN
    c->rip = (Word) os->__rip;
    c->flags = (Word) os->__rflags;
#elif os_LINUX
    c->rip = (Word) os->rip;
    c->flags = (Word) os->eflags;
#elif os_SOLARIS
    c->rip = (Word) os[REG_RIP];
    c->flags = (Word) os[REG_RFL];
#elif os_MAXVE
    c->rip = (Word) os->rip;
    c->flags = (Word) os->flags;
#else
    c_UNIMPLEMENTED();
#endif
}

void isa_printCanonicalIntegerRegisters(isa_CanonicalIntegerRegisters c) {
#define PRINT_REG(name, field) log_println(name " = %p [%ld]", c->field, c->field)
    PRINT_REG("RAX", rax);
    PRINT_REG("RCX", rcx);
    PRINT_REG("RDX", rdx);
    PRINT_REG("RBX", rbx);
    PRINT_REG("RSP", rsp);
    PRINT_REG("RDI", rdi);
    PRINT_REG("R8 ", r8);
    PRINT_REG("R9 ", r9);
    PRINT_REG("R10", r10);
    PRINT_REG("R11", r11);
    PRINT_REG("R12", r12);
    PRINT_REG("R13", r13);
    PRINT_REG("R14", r14);
    PRINT_REG("R15", r15);
#undef PRINT_REG
}

void isa_printCanonicalFloatingPointRegisters(isa_CanonicalFloatingPointRegisters c) {
    log_println("XMM0  = %p [%g]", c->xmm0, c->xmm0);
    log_println("XMM1  = %p [%g]", c->xmm1, c->xmm1);
    log_println("XMM2  = %p [%g]", c->xmm2, c->xmm2);
    log_println("XMM3  = %p [%g]", c->xmm3, c->xmm3);
    log_println("XMM4  = %p [%g]", c->xmm4, c->xmm4);
    log_println("XMM5  = %p [%g]", c->xmm5, c->xmm5);
    log_println("XMM6  = %p [%g]", c->xmm6, c->xmm6);
    log_println("XMM7  = %p [%g]", c->xmm7, c->xmm7);
    log_println("XMM8  = %p [%g]", c->xmm8, c->xmm8);
    log_println("XMM9  = %p [%g]", c->xmm9, c->xmm9);
    log_println("XMM10 = %p [%g]", c->xmm10, c->xmm10);
    log_println("XMM11 = %p [%g]", c->xmm11, c->xmm11);
    log_println("XMM12 = %p [%g]", c->xmm12, c->xmm12);
    log_println("XMM13 = %p [%g]", c->xmm13, c->xmm13);
    log_println("XMM14 = %p [%g]", c->xmm14, c->xmm14);
    log_println("XMM15 = %p [%g]", c->xmm15, c->xmm15);
}

void isa_printCanonicalStateRegisters(isa_CanonicalStateRegisters canonicalStateRegisters) {
    log_println("rip   = %p [%ld]", canonicalStateRegisters->rip,canonicalStateRegisters->rip);
    log_println("flags = %p [%ld]", canonicalStateRegisters->flags, canonicalStateRegisters->flags);
}
