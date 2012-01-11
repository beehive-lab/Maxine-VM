/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;

public abstract class PointerIndexAndHeaderVisitor extends PointerIndexVisitor {

    protected final int HUB_WORD_INDEX = Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt() >> Word.widthValue().log2numberOfBytes;
    /**
     * Word index to the first element of a reference array from its origin.
     */
    protected final int FIRST_ELEMENT_INDEX = Layout.referenceArrayLayout().getElementOffsetInCell(0).toInt() >> Kind.REFERENCE.width.log2numberOfBytes;

    public PointerIndexAndHeaderVisitor() {
        super();
    }

    final protected Hub getHub(Pointer origin) {
        return UnsafeCast.asHub(origin.getReference(HUB_WORD_INDEX));
    }

}
