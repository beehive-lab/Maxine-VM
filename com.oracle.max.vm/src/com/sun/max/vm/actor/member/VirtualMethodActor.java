/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Class methods with dynamic dispatch on the receiver.
 */
public class VirtualMethodActor extends ClassMethodActor {

    public VirtualMethodActor(Utf8Constant name,
                    SignatureDescriptor descriptor,
                    int flags,
                    CodeAttribute codeAttribute, String intrinsic) {
        super(name,
              descriptor,
              flags,
              codeAttribute, intrinsic);
    }

    public static final int NONVIRTUAL_VTABLE_INDEX = -2;
    public static final int INVALID_VTABLE_INDEX = -4;

    @CONSTANT
    private int vTableIndex = INVALID_VTABLE_INDEX;

    // FIXME: temp debugging
    public boolean hadDeopt; // true if was deopt at least once.

    /**
     * Gets the index of this method in the vtable of its holder's class hierarchy.
     * A negative return value implies that this method in not an entry in any vtable.
     * This is the case for private methods and constructors.
     */
    @INLINE
    public final int vTableIndex() {
        return vTableIndex;
    }

    public void setVTableIndex(int vTableIndex) {
        assert this.vTableIndex == INVALID_VTABLE_INDEX;
        this.vTableIndex = vTableIndex;
    }

    /**
     * Sets the vtable index for a substitute method to be the same as the vtable index for
     * the original. This is required so that virtual dispatch works properly at runtime
     * when the receiver object is of the original type.
     */
    @HOSTED_ONLY
    public void resetVTableIndexForSubstitute(int vTableIndex) {
        assert ((VirtualMethodActor) original()).vTableIndex() == vTableIndex;
        assert this.vTableIndex != INVALID_VTABLE_INDEX;
        this.vTableIndex = vTableIndex;
    }
}
