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
package com.sun.max.vm.runtime.sparc;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;


/**
 * Dynamic Trampoline Exit for SPARC64.
 * No manipulation of the stack frame are needed. The trampoline epilogue produced by the optimizing compiler is enough.
 *
 * @author Laurent Daynes
 */
public class SPARCDynamicTrampolineExit extends DynamicTrampolineExit {

    /**
     * Returns the address where the trampoline will exit to.
     *
     * @param dynamicTrampoline the dynamic trampoline to exit
     * @param vtableEntryPoint the target method entry the trampoline should exit to.
     * @param stackPointer the stack pointer of the dynamic trampoline
     */
    @Override
    public Address trampolineReturnAddress(DynamicTrampoline dynamicTrampoline, Address vtableEntryPoint, Pointer stackPointer) {
        return  vtableEntryPoint.plus(OPTIMIZED_ENTRY_POINT.offsetFromCodeStart() - VTABLE_ENTRY_POINT.offsetFromCodeStart());
    }
}
