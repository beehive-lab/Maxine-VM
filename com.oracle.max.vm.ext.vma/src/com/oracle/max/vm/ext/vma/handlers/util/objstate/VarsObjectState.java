/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.xohm.*;
import com.sun.max.vm.reference.*;

public class VarsObjectState extends SimpleObjectState implements ObjectVars {

    private final int slots;

    public VarsObjectState() {
        LayoutScheme scheme = VMConfiguration.activeConfig().layoutScheme();
        slots = ((XOhmGeneralLayout) scheme.generalLayout).xtraCount - 1;
    }

    @Override
    public void writeVar(Object obj, int slot, Word value) {
        checkIndex(slot);
        XOhmGeneralLayout.Static.writeXtra(Reference.fromJava(obj), slot + 1, value);
    }

    @Override
    public Word readVar(Object obj, int slot) {
        checkIndex(slot);
        return XOhmGeneralLayout.Static.readXtra(Reference.fromJava(obj), slot + 1);
    }

    private void checkIndex(int slot) {
        if (slot >= slots) {
            throw new ArrayIndexOutOfBoundsException(slot);
        }
    }

}
