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
#include "isa.h"

void isa_decanonicalizeSignalIntegerRegisters(isa_OsSignalCanonicalIntegerRegisters canonicalIntegerRegisters, isa_OsSignalIntegerRegisters osSignalIntegerRegisters) {
#if isa_AMD64
	amd64_decanonicalizeSignalIntegerRegisters(canonicalIntegerRegisters, osSignalIntegerRegisters);
#elif isa_SPARC
	sparc_decanonicalizeSignalIntegerRegisters(canonicalIntegerRegisters, osSignalIntegerRegisters);
#else
#error Unimplemented
#endif
}

void isa_canonicalizeSignalIntegerRegisters(isa_OsSignalIntegerRegisters osSignalIntegerRegisters, isa_OsSignalCanonicalIntegerRegisters canonicalIntegerRegisters) {
#if isa_AMD64
	amd64_canonicalizeSignalIntegerRegisters(osSignalIntegerRegisters, canonicalIntegerRegisters);
#elif isa_SPARC
   sparc_canonicalizeSignalIntegerRegisters(osSignalIntegerRegisters, canonicalIntegerRegisters);
#else
#error Unimplemented
#endif
}

void isa_canonicalizeSignalFloatingPointRegisters(isa_OsSignalFloatingPointRegisters osSignalFloatingPointRegisters, isa_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters) {
#if isa_AMD64
	amd64_canonicalizeSignalFloatingPointRegisters(osSignalFloatingPointRegisters, canonicalFloatingPointRegisters);
#elif isa_SPARC
	sparc_canonicalizeSignalFloatingPointRegisters(osSignalFloatingPointRegisters, canonicalFloatingPointRegisters);
#else
#error Unimplemented
#endif
}

void isa_printCanonicalIntegerRegisters(isa_CanonicalIntegerRegisters canonicalIntegerRegisters) {
#if isa_AMD64
    amd64_printCanonicalIntegerRegisters(canonicalIntegerRegisters);
#elif isa_SPARC
    sparc_printCanonicalIntegerRegisters(canonicalIntegerRegisters);
#else
#error Unimplemented
#endif
}

void isa_printCanonicalFloatingPointRegisters(isa_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters) {
#if isa_AMD64
    amd64_printCanonicalFloatingPointRegisters(canonicalFloatingPointRegisters);
#elif isa_SPARC
    sparc_printCanonicalFloatingPointRegisters(canonicalFloatingPointRegisters);
#else
#error Unimplemented
#endif
}

void isa_canonicalizeTeleIntegerRegisters(isa_OsTeleIntegerRegisters osTeleIntegerRegisters, isa_CanonicalIntegerRegisters canonicalIntegerRegisters) {
#if isa_AMD64
	amd64_canonicalizeTeleIntegerRegisters(osTeleIntegerRegisters, canonicalIntegerRegisters);
#elif isa_IA32
	ia32_canonicalizeTeleIntegerRegisters(osTeleIntegerRegisters, canonicalIntegerRegisters);
#elif isa_POWER
	power_canonicalizeTeleIntegerRegisters(osTeleIntegerRegisters, canonicalIntegerRegisters);
#elif isa_SPARC
	sparc_canonicalizeTeleIntegerRegisters(osTeleIntegerRegisters, canonicalIntegerRegisters);
#else
#error Unimplemented
#endif
}

void isa_canonicalizeTeleStateRegisters(isa_OsTeleStateRegisters osTeleStateRegisters, isa_CanonicalStateRegisters canonicalStateRegisters) {
#if isa_AMD64
	amd64_canonicalizeTeleStateRegisters(osTeleStateRegisters, canonicalStateRegisters);
#elif isa_SPARC
	sparc_canonicalizeTeleStateRegisters(osTeleStateRegisters, canonicalStateRegisters);
#else
#error Unimplemented
#endif
}

void isa_canonicalizeTeleFloatingPointRegisters(isa_OsTeleFloatingPointRegisters osTeleFloatingPointRegisters, isa_CanonicalFloatingPointRegisters canonicalFloatingPointRegisters) {
#if isa_AMD64
	amd64_canonicalizeTeleFloatingPointRegisters(osTeleFloatingPointRegisters, canonicalFloatingPointRegisters);
#elif isa_SPARC
	sparc_canonicalizeTeleFloatingPointRegisters(osTeleFloatingPointRegisters, canonicalFloatingPointRegisters);
#else
#error Unimplemented
#endif
}
