/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;
/**
 * A policy object that helps with taking decisions with respect to when to refill a tlab on allocation failure, what size the tlab should have on next refill etc....
 * TLABRefillPolicy are stored in a thread local variable TLAB_REFILL_POLICY if tlabs are being used. Threads may refer to the same TLAB policy or
 * to one individual policy if the policy allows each thread to have their TLAB evolves differently.
 *
 * @author Laurent Daynes
 */
public abstract class TLABRefillPolicy {
    /**
     * Thread local holding the thread's refill policy.
     */
    private static final VmThreadLocal TLAB_REFILL_POLICY = new VmThreadLocal("TLAB_REFILL_POLICY", true, "Refill policy for thread's TLABs");

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

    @UNSAFE_CAST
    private static native TLABRefillPolicy asTLABRefillPolicy(Object object);

    @INLINE
    public static TLABRefillPolicy getForCurrentThread(Pointer enabledVmThreadLocals) {
        final Reference reference = TLAB_REFILL_POLICY.getVariableReference(enabledVmThreadLocals);
        if (reference.isZero()) {
            return null;
        }
        final TLABRefillPolicy tlabRefillPolicy = asTLABRefillPolicy(reference.toJava());
        return tlabRefillPolicy;
    }

    @INLINE
    public static void setForCurrentThread(Pointer enabledVmThreadLocals, TLABRefillPolicy policy) {
        TLAB_REFILL_POLICY.setVariableReference(enabledVmThreadLocals, Reference.fromJava(policy));
    }

}
