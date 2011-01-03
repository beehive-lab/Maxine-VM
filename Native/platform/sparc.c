/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
#include "isa.h"
#include "log.h"

void isa_canonicalizeTeleIntegerRegisters(isa_OsTeleIntegerRegisters os, isa_CanonicalIntegerRegisters c) {
#if os_SOLARIS
    // See procfs_isa.h
    int r = R_G0;
    Word *p = &c->g0;
    while (r <= R_I7) {
        p[r] = (Word) os[r];
        r++;
    }
#else
    c_UNIMPLEMENTED();
#endif
}

void isa_canonicalizeTeleStateRegisters(isa_OsTeleStateRegisters os, isa_CanonicalStateRegisters c) {
#if os_SOLARIS
    c->ccr = (Word) os[R_CCR];
    c->pc = (Word) os[R_PC];
    c->npc = (Word) os[R_nPC];
#else
    c_UNIMPLEMENTED();
#endif
}

void isa_canonicalizeTeleFloatingPointRegisters(isa_OsTeleFloatingPointRegisters os, isa_CanonicalFloatingPointRegisters c) {
#if os_SOLARIS
    int r = 0;
    while (r < 32) {
        c->dRegs[r] = (Word) os->pr_fr.pr_regs[r];
        r++;
    }
#else
    c_UNIMPLEMENTED();
#endif
}

void isa_printCanonicalIntegerRegisters(isa_CanonicalIntegerRegisters c) {
#if os_SOLARIS
    // See procfs_isa.h
    static char registerNames[] = "GOLI";
    int r = R_G0;
    Word *p = &c->g0;
    while (r <= R_I7) {
        char rn = registerNames[r / 8];
        for (int i = 0; i < 8; i++, r++) {
            log_println("%%%c%d = %p [%ld]", rn, i, p[r], p[r]);
        }
    }
#else
    c_UNIMPLEMENTED();
#endif
}

void isa_printCanonicalFloatingPointRegisters(isa_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters) {
#if os_SOLARIS
#define PRINT_FPR(r) log_println("F%-2d = %p [%lf]", r, canonicalFloatingPointRegisters->dRegs[r], canonicalFloatingPointRegisters->dRegs[r])
    int r = 0;
    while (r < 32) {
        PRINT_FPR(r);
        r++;
    }
#else
    c_UNIMPLEMENTED();
#endif
}

void isa_printCanonicalStatePointRegisters(isa_CanonicalStateRegisters canonicalStateRegisters) {
#if os_SOLARIS
    log_println("%%ccr = %p [%ld]", canonicalStateRegisters->ccr, canonicalStateRegisters->ccr);
    log_println("%%pc  = %p [%ld]", canonicalStateRegisters->pc, canonicalStateRegisters->pc);
    log_println("%%npc = %p [%ld]", canonicalStateRegisters->npc, canonicalStateRegisters->npc);
#else
    c_UNIMPLEMENTED();
#endif
}
