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
package com.sun.max.vm.trampoline;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;


public abstract class TrampolineGenerator {

    protected TrampolineGenerator() {
    }

    /**
     * Indexed sequence of available trampolines. The trampoline for a method with index i is the ith element of the sequence.
     */
    private final AppendableIndexedSequence<DynamicTrampoline> trampolines = new ArrayListSequence<DynamicTrampoline>();

    private void traceDynamicTrampolines(int i) {
        if (i % 1000 == 0 && i != 0) {
            Trace.line(1, "extending trampolines up to " + i);
        }
    }

    public abstract DynamicTrampoline createTrampoline(int dispatchTableIndex);

    public synchronized Address makeCallEntryPoint(int dispatchTableIndex) {
        if (trampolines.length() <= dispatchTableIndex) {
            for (int i = trampolines.length(); i <= dispatchTableIndex; i++) {
                traceDynamicTrampolines(i);
                trampolines.append(createTrampoline(i));
            }
        }
        return VTABLE_ENTRY_POINT.in(trampolines.get(dispatchTableIndex).trampolineTargetMethod());
    }

}
