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
