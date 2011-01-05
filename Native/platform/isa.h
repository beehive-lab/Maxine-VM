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

/**
 * Types and functions for dealing with type ISA specific registers.
 *
 * @author Bernd Mathiske
 */
#ifndef __isa_h__
#define __isa_h__ 1

#define isa_SPARC              0
#define isa_IA32               0
#define isa_AMD64              0
#define isa_POWER              0

#if defined(__sparc) || defined(sparc)
#   undef  isa_SPARC
#   define isa_SPARC           1
#   define isa_IDENTIFIER      SPARC

#   include "sparc.h"

    typedef sparc_OsTeleIntegerRegisters isa_OsTeleIntegerRegisters;
    typedef struct sparc_CanonicalIntegerRegisters isa_CanonicalIntegerRegistersStruct;
    typedef sparc_CanonicalIntegerRegisters isa_CanonicalIntegerRegisters;

    typedef sparc_OsTeleFloatingPointRegisters isa_OsTeleFloatingPointRegisters;
    typedef struct sparc_CanonicalFloatingPointRegisters isa_CanonicalFloatingPointRegistersStruct;
    typedef sparc_CanonicalFloatingPointRegisters isa_CanonicalFloatingPointRegisters;

    typedef sparc_OsTeleStateRegisters isa_OsTeleStateRegisters;
    typedef struct sparc_CanonicalStateRegisters isa_CanonicalStateRegistersStruct;
    typedef sparc_CanonicalStateRegisters isa_CanonicalStateRegisters;

#elif defined(__x86_64) || defined(amd64)
#   undef  isa_AMD64
#   define isa_AMD64           1
#   define isa_IDENTIFIER      AMD64

#   include "amd64.h"

    typedef amd64_OsTeleIntegerRegisters isa_OsTeleIntegerRegisters;
    typedef struct amd64_CanonicalIntegerRegisters isa_CanonicalIntegerRegistersStruct;
    typedef amd64_CanonicalIntegerRegisters isa_CanonicalIntegerRegisters;

    typedef amd64_OsTeleFloatingPointRegisters isa_OsTeleFloatingPointRegisters;
    typedef struct amd64_CanonicalFloatingPointRegisters isa_CanonicalFloatingPointRegistersStruct;
    typedef amd64_CanonicalFloatingPointRegisters isa_CanonicalFloatingPointRegisters;

    typedef amd64_OsTeleStateRegisters isa_OsTeleStateRegisters;
    typedef struct amd64_CanonicalStateRegisters isa_CanonicalStateRegistersStruct;
    typedef amd64_CanonicalStateRegisters isa_CanonicalStateRegisters;

#elif defined(__x86) || defined(_X86_) || defined(i386) || defined(ia32)
#   undef  isa_IA32
#   define isa_IA32            1
#   define isa_IDENTIFIER      IA32

#   include "ia32.h"

    typedef ia32_OsTeleIntegerRegisters isa_OsTeleIntegerRegisters;
    typedef struct ia32_CanonicalIntegerRegisters isa_CanonicalIntegerRegistersStruct;
    typedef ia32_CanonicalIntegerRegisters isa_CanonicalIntegerRegisters;

    typedef ia32_OsTeleFloatingPointRegisters isa_OsTeleFloatingPointRegisters;
    typedef struct ia32_CanonicalFloatingPointRegisters isa_CanonicalFloatingPointRegistersStruct;
    typedef ia32_CanonicalFloatingPointRegisters isa_CanonicalFloatingPointRegisters;

    typedef ia32_OsTeleStateRegisters isa_OsTeleStateRegisters;
    typedef struct ia32_CanonicalStateRegisters isa_CanonicalStateRegistersStruct;
    typedef ia32_CanonicalStateRegisters isa_CanonicalStateRegisters;

#elif defined(__ppc__) || defined(__POWERPC__) || defined(_ARCH_PPC) || defined(power)
#   undef  isa_POWER
#   define isa_POWER           1
#   define isa_IDENTIFIER      POWER

#   include "power.h"

    typedef power_OsTeleIntegerRegisters isa_OsTeleIntegerRegisters;
    typedef struct power_CanonicalIntegerRegisters isa_CanonicalIntegerRegistersStruct;
    typedef power_CanonicalIntegerRegisters isa_CanonicalIntegerRegisters;

#else
#   error
#endif

/*
 * Canonicalization of registers in normal running mode.
 */
extern void isa_canonicalizeTeleIntegerRegisters(isa_OsTeleIntegerRegisters osTeleIntegerRegisters, isa_CanonicalIntegerRegisters canonicalIntegerRegisters);
extern void isa_canonicalizeTeleFloatingPointRegisters(isa_OsTeleFloatingPointRegisters osTeleFloatingPointRegisters, isa_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);
extern void isa_canonicalizeTeleStateRegisters(isa_OsTeleStateRegisters osTeleStateRegisters, isa_CanonicalStateRegisters canonicalStateRegisters);

/*
 * Printing of canonical registers.
 */
extern void isa_printCanonicalIntegerRegisters(isa_CanonicalIntegerRegisters canonicalIntegerRegisters);
extern void isa_printCanonicalFloatingPointRegisters(isa_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters);
extern void isa_printCanonicalStateRegisters(isa_CanonicalStateRegisters canonicalStateRegisters);

#endif /*__isa_h__*/
