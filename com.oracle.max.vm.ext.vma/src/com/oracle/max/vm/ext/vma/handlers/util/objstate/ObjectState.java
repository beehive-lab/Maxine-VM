/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers.util.objstate;

import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.xohm.*;
import com.sun.max.vm.reference.*;

/**
 * The basic implementation of state used by handlers for storing
 * miscellaneous data about an object. The state is stored as a single
 * {@link Word} value in a VMA-specific word in the object header.
 * The value must not be a reference, as this word is not scanned by the GC.
 */
public abstract class ObjectState {
    /**
     * Read and return the state value or zero if {@code obj == null}.
     */
    public Word readState(Object obj) {
        return readState(Reference.fromJava(obj));
    }

    /**
     * Variant using a {@link Reference}.
     */
    public Word readState(Reference objRef) {
        return XOhmGeneralLayout.Static.readXtra(objRef);
    }

    /**
     * Update the state value for {@code obj} to {@code state}. assert: {@code obj != null}
     */
    public void writeState(Object obj, Word state) {
        writeState(Reference.fromJava(obj), state);
    }

    /**
     * Variant using a {@link Reference}.
     */
    public void writeState(Reference objRef, Word state) {
        XOhmGeneralLayout.Static.writeXtra(objRef, state);
    }
}
