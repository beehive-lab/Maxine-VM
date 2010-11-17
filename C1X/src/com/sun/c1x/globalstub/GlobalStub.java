/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.globalstub;

import static com.sun.cri.ci.CiKind.*;

import com.sun.cri.ci.*;

/**
 * A global stub is a shared routine that performs an operation on behalf of compiled code.
 * Typically the routine is too large to inline, is infrequent, or requires runtime support.
 * Global stubs are called with a callee-save convention; the global stub must save any
 * registers it may destroy and then restore them upon return. This allows the register
 * allocator to ignore calls to global stubs. Parameters to global stubs are
 * passed on the stack in order to preserve registers for the rest of the code.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class GlobalStub {

    public enum Id {

        fneg(Float, Float),
        dneg(Double, Double),
        f2i(Int, Float),
        f2l(Long, Float),
        d2i(Int, Double),
        d2l(Long, Double);

        public final CiKind resultKind;
        public final CiKind[] arguments;

        private Id(CiKind resultKind, CiKind... args) {
            this.resultKind = resultKind;
            this.arguments = args;
        }
    }

    public final Id id;
    public final CiKind resultKind;
    public final Object stubObject;
    public final int argsSize;
    public final int[] argOffsets;
    public final int resultOffset;

    public GlobalStub(Id id, CiKind resultKind, Object stubObject, int argsSize, int[] argOffsets, int resultOffset) {
        this.id = id;
        this.resultKind = resultKind;
        this.stubObject = stubObject;
        this.argsSize = argsSize;
        this.argOffsets = argOffsets;
        this.resultOffset = resultOffset;
    }

}
