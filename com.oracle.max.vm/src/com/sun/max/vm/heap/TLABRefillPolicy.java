/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;
/**
 * A policy object that helps with taking decisions with respect to when to refill a tlab on allocation failure, what size the tlab should have on next refill etc....
 * TLABRefillPolicy are stored in a thread local variable TLAB_REFILL_POLICY if tlabs are being used. Threads may refer to the same TLAB policy or
 * to one individual policy if the policy allows each thread to have their TLAB evolves differently.
 */
public abstract class TLABRefillPolicy {
    /**
     * Thread local holding the thread's refill policy.
     */
    private static final VmThreadLocal TLAB_REFILL_POLICY = new VmThreadLocal("TLAB_REFILL_POLICY", true, "Refill policy for thread's TLABs", Nature.Single);

    /**
     * Storage for saving the TLAB's allocation mark. Used when disabling / enabling allocation.
     */
    private Address savedTlabTop = Address.zero();

    public void saveTlabTop(Address tlabTop) {
        savedTlabTop = tlabTop;
    }

    public Address getSavedTlabTop() {
        return savedTlabTop;
    }

    /**
     * Return policy decision regarding whether the TLAB for the current thread should be refilled.
     * @param size size of the allocation request that causes the request to refill the TLAB
     * @param allocationMark allocation mark of the TLAB
     * @return
     */
    public abstract boolean shouldRefill(Size size, Pointer allocationMark);

    /**
     * Returns the size the TLAB should have on next refill.
     * @return
     */
    public abstract Size nextTlabSize();

    @INTRINSIC(UNSAFE_CAST)
    private static native TLABRefillPolicy asTLABRefillPolicy(Object object);

    @INLINE
    public static TLABRefillPolicy getForCurrentThread(Pointer etla) {
        final Reference reference = TLAB_REFILL_POLICY.loadRef(etla);
        if (reference.isZero()) {
            return null;
        }
        final TLABRefillPolicy tlabRefillPolicy = asTLABRefillPolicy(reference.toJava());
        return tlabRefillPolicy;
    }

    @INLINE
    public static void setForCurrentThread(Pointer etla, TLABRefillPolicy policy) {
        TLAB_REFILL_POLICY.store(etla, Reference.fromJava(policy));
    }

}
