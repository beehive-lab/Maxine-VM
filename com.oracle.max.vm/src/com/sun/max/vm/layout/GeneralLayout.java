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
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.reference.*;

/**
 * A general layout contains methods for manipulating an object without knowing whether
 * it is a tuple or an array or a hub.
 */
public interface GeneralLayout {

    String name();

    Pointer cellToOrigin(Pointer cell);

    Pointer originToCell(Pointer origin);

    Layout.Category category(Accessor accessor);

    boolean isArray(Accessor accessor);

    boolean isTuple(Accessor accessor);

    boolean isHybrid(Accessor accessor);

    SpecificLayout specificLayout(Accessor accessor);

    /**
     * Gets the size of the cell allocated for a given object. This include the size of the object's body,
     * its header as well as any extra bytes required to satisfy alignment constraints.
     */
    Size size(Accessor accessor);

    Reference readHubReference(Accessor accessor);

    Word readHubReferenceAsWord(Accessor accessor);

    void writeHubReference(Accessor accessor, Reference hubReference);

    Word readMisc(Accessor accessor);

    void writeMisc(Accessor accessor, Word value);

    Word compareAndSwapMisc(Accessor accessor, Word expectedValue, Word newValue);

    Offset getOffsetFromOrigin(HeaderField headerField);

    Reference forwarded(Reference ref);

    /**
     * @return the forward reference stored by the GC in the object, null if none stored
     */
    Reference readForwardRef(Accessor accessor);

    Reference readForwardRefValue(Accessor accessor);

    void writeForwardRef(Accessor accessor, Reference forwardRef);

    Reference compareAndSwapForwardRef(Accessor accessor, Reference suspectedRef, Reference forwardRef);
}
