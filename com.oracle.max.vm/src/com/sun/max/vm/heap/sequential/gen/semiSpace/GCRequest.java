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
package com.sun.max.vm.heap.sequential.gen.semiSpace;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

/**
 * A thread-local GC request.
 *
 */
public class GCRequest {
    private static final VmThreadLocal GC_REQUEST = new VmThreadLocal("GC_REQUEST", true, "Request submitted to GC", Nature.Single);

    /**
     * A single link list of all request to be submitted.
     */
    private static GCRequest pending;

    GCRequest next;
    Size requestedSize;
    boolean outOfMemory;
    boolean cancelled;

    @INTRINSIC(UNSAFE_CAST)
    private static native GCRequest asGCRequest(Object object);

    static GCRequest getForCurrentThread() {
        final Pointer etla = ETLA.load(VmThread.currentTLA());
        final Reference reference = GC_REQUEST.loadRef(etla);
        if (reference.isZero()) {
            return null;
        }
        return asGCRequest(reference.toJava());
    }

    static void setForCurrentThread(GCRequest gcRequest) {
        final Pointer etla = ETLA.load(VmThread.currentTLA());
        GC_REQUEST.store(etla, Reference.fromJava(gcRequest));
    }
}
