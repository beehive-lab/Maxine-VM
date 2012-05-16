/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;


/**
 * Helper class for handling multi-dimensional arrays when it suffices to
 * treat them as if they had been allocated as single-dimensional arrays.
 */
public class MultiNewArrayHelper {
    public static void handleMultiArray(VMAdviceHandler handler, Object array) {
        final Reference objRef = Reference.fromJava(array);
        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));
        if (hub.classActor.componentClassActor().isArrayClass()) {
            final int length = Layout.readArrayLength(objRef);
            for (int i = 0; i < length; i++) {
                Reference subRef = Layout.getReference(objRef, i);
                if (!subRef.isZero()) {
                    handler.adviseAfterNewArray(subRef.toJava(), Layout.readArrayLength(subRef));
                }
            }
        }
    }


}
