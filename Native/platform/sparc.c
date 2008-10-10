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
#include "sparc.h"
#include <debug.h>

void sparc_decanonicalizeSignalIntegerRegisters(sparc_OsSignalCanonicalIntegerRegisters c, sparc_OsSignalIntegerRegisters os) {
#if os_SOLARIS
  int r = REG_G1;
  Word *p = &c->g1;
  while (r <= REG_O7) {
	 os[r] = *p;
	 p++;
	 r++;
  }
#else
#error Unimplemented
#endif
}

void sparc_canonicalizeSignalIntegerRegisters(sparc_OsSignalIntegerRegisters os, sparc_OsSignalCanonicalIntegerRegisters c) {
#if os_SOLARIS
  int r = REG_G1;
  c->g0 = 0;               // always
  Word *p = &c->g1;
  while (r <= REG_O7) {
	 *p = (Word) os[r];
    p++;
	 r++;
  }
#else
#error Unimplemented
#endif
}

void sparc_canonicalizeSignalFloatingPointRegisters(sparc_OsSignalFloatingPointRegisters os, sparc_CanonicalFloatingPointRegisters c) {
#if os_SOLARIS
  int r = 0;
  Word *osRegister = (Word*) &os->fpu_fr.fpu_dregs[0];
  while (r < 32) {
	 c->dRegs[r] = osRegister[r];
	 r++;
  }
#else
  c_unimplemented();
#endif
}

void sparc_printCanonicalIntegerRegisters(sparc_CanonicalIntegerRegisters c) {
#if os_SOLARIS
  // See procfs_isa.h
  static char registerNames[] = "GOLI";
  int r = R_G0;
  Word *p = &c->g0;
  while (r <= R_I7) {
    char rn = registerNames[r / 8]; 
    for (int i = 0; i < 8; i++, r++) {
   	 debug_println("%%%c%d = 0x%016lx [%ld]", rn, i, p[r], p[r]);
	 }
  }
#else
#error Unimplemented
#endif
}

void sparc_canonicalizeTeleIntegerRegisters(sparc_OsTeleIntegerRegisters os, sparc_CanonicalIntegerRegisters c) {
#if os_SOLARIS
  // See procfs_isa.h
  int r = R_G0;
  Word *p = &c->g0;
  while (r <= R_I7) {
	 p[r] = (Word) os[r];
	 r++;
  }
#else
#error Unimplemented
#endif
}

void sparc_canonicalizeTeleStateRegisters(sparc_OsTeleStateRegisters os, sparc_CanonicalStateRegisters c) {
#if os_SOLARIS
  c->ccr = os[R_CCR];
  c->pc = os[R_PC];
  c->npc = os[R_nPC];
#else
#error Unimplemented
#endif
}

void sparc_canonicalizeTeleFloatingPointRegisters(sparc_OsTeleFloatingPointRegisters os, sparc_CanonicalFloatingPointRegisters c) {
#if os_SOLARIS
  int r = 0;
  while (r < 32) {
	 c->dRegs[r] = os->pr_fr.pr_regs[r];
	 r++;
  }
#else
#error Unimplemented
#endif
}


