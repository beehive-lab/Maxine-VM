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
/*VCSID=99819eb9-62c0-42be-a46a-474b6eb4ded1*/
package com.sun.max.vm.compiler.eir.ia32.unix;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.ia32.*;

/**
 * The native ABI used by the Solaris OS.
 *
 * @author Bernd Mathiske
 */
public class UnixIA32EirNativeABI extends UnixIA32EirCFunctionABI {

    public UnixIA32EirNativeABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    public PoolSet<IA32EirRegister> callerSavedRegisters() {
        // We override here to support safepoints.
        // Normally the native ABI's caller-saved registers would be identical to those of the CFunction ABI,
        // i.e. a strict subset of all allocatable registers.
        // BUT then there could be references in non-caller-saved registers on exit from a native method.
        // This would make hard safepoints look different to the GC from staying inside native code.
        // To prevent the GC from incurring races between inconsistent register maps,
        // we make all registers caller-saved around native calls.
        // Then the GC never needs to track any registers no matter
        // whether mutator thread execution stays in native code or returns from it and then hits a hard safepoint.
        // The idea is that any allocatable register will be refilled from an upto-date stack slot
        // before any use after the safepoint.
        return allocatableRegisters();
    }
}
