/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.heap.semi;

import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.Pointer;
import com.sun.max.unsafe.Size;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.heap.HeapScheme;
import com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme;

/**
 * An extension of {@link SemiSpaceHeapScheme} that supports the tracking of object creation
 * and lifetime analysis by implementing the appropriate methods in {@link HeapScheme}, i.e.,
 * {@link HeapScheme#trackCreation(Pointer, Hub, boolean)} and {@link HeapScheme#trackLifetime(Pointer)}.
 *
 * Tracking only occurs if the option {@link VMAJavaRunScheme#objectAnalysis} is set
 * and only once the VM reaches the {@link MaxineVM.Phase#RUNNING} phase.
 *
 */

public class VMASemiSpaceHeapScheme extends SemiSpaceHeapScheme {

    @INLINE(override = true)
    @Override
    public void trackLifetime(Pointer cell) {
        /*
         * This method is called in the VMOperation thread (which normally does not have
         * tracking enabled). It should be fast and must not allocate. Therefore
         * there is no need or requirement to enable/disable.
         */
        if (VMAJavaRunScheme.isVMAdvising()) {
            VMAJavaRunScheme.adviceHandler().gcSurvivor(cell);
        }
    }

    @Override
    public boolean collectGarbage(Size requestedFreeSpace) {
        final boolean result = super.collectGarbage(requestedFreeSpace);
        if (VMAJavaRunScheme.isAdvising()) {
            VMAJavaRunScheme.disableAdvising();
            VMAJavaRunScheme.adviceHandler().adviseAfterGC();
            VMAJavaRunScheme.enableAdvising();
        }
        return result;
    }


}
