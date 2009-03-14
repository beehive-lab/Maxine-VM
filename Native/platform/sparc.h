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
/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
#ifndef __sparc_h__
#define __sparc_h__ 1

#include "os.h"

#if os_LINUX
#   include <sys/user.h>
	typedef struct user_regs_struct *sparc_OsTeleIntegerRegisters;
#elif os_SOLARIS
#   include <sys/types.h>
#   include <sys/procfs.h>
	typedef prgreg_t *sparc_OsTeleIntegerRegisters;
	typedef prgreg_t *sparc_OsTeleStateRegisters;
	typedef prfpregset_t *sparc_OsTeleFloatingPointRegisters;
#endif

#include "word.h"

typedef struct sparc_CanonicalIntegerRegisters {
  Word g0;  /* r[0] = prgreg_t[] */
  Word g1;
  Word g2;
  Word g3;
  Word g4;
  Word g5;
  Word g6;
  Word g7;
  Word o0;  /* r[8] */
  Word o1;
  Word o2;
  Word o3;
  Word o4;
  Word o5;
  Word o6;
  Word o7;
  Word l0;  /* r[16] */
  Word l1;
  Word l2;
  Word l3;
  Word l4;
  Word l5;
  Word l6;
  Word l7;
  Word i0;  /* r[24] */
  Word i1;
  Word i2;
  Word i3;
  Word i4;
  Word i5;
  Word i6;
  Word i7;
} sparc_CanonicalIntegerRegistersAggregate, *sparc_CanonicalIntegerRegisters;

typedef struct sparc_CanonicalFloatingPointRegisters {
#ifdef	__sparcv9
  Word dRegs[32]; /* Double precision floating point register, f0, f2, ... f62. */
#else
#endif
} sparc_CanonicalFloatingRegistersAggregate, *sparc_CanonicalFloatingPointRegisters;

typedef struct sparc_CanonicalStateRegisters {
  Word ccr; /* Condition Code Register */
  Word pc;  /* PC register */
  Word npc; /* nPC register */
} sparc_CanonicalStateRegistersAggregate, *sparc_CanonicalStateRegisters;

extern void sparc_canonicalizeTeleIntegerRegisters(sparc_OsTeleIntegerRegisters osTeleIntegerRegisters, sparc_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void sparc_canonicalizeTeleFloatingPointRegisters(sparc_OsTeleFloatingPointRegisters osTeleFloatingPointRegisters, sparc_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void sparc_canonicalizeTeleStateRegisters(sparc_OsTeleStateRegisters osTeleStateRegisters, sparc_CanonicalStateRegisters canonicalStateRegisters);

extern void sparc_printCanonicalIntegerRegisters(sparc_CanonicalIntegerRegisters canonicalIntegerRegisters);

extern void sparc_printCanonicalFloatingPointRegisters(sparc_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);

extern void sparc_printCanonicalStatePointRegisters(sparc_CanonicalTeleStateRegisters canonicalStateRegisters);

#endif /*__sparc_h__*/
