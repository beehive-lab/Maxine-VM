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

#endif /*__sparc_h__*/
