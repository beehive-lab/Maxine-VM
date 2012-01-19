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
package com.sun.max.tele.reference;

import java.util.concurrent.atomic.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;

/**
 * Special implementations of VM {@link Reference}s to objects that permit reuse of many
 * VM classes and schemas in the context of non-local inspection.
 *
 * @see VmReferenceManager
 */
public abstract class RemoteReference extends Reference {

    private final TeleVM vm;

    private long refOID = 0;

    private static final AtomicLong nextOID = new AtomicLong(1);

    protected RemoteReference forwardedTeleRef = null;

    protected boolean collectedByGC = false;

    protected RemoteReference(TeleVM vm) {
        this.vm = vm;
    }

    public TeleVM vm() {
        return vm;
    }
    /**
     * @return a non-zero integer uniquely identifying the referred-to object in the VM, assigned lazily
     */
    public synchronized long makeOID() {
        if (refOID == 0) {
            refOID = nextOID.incrementAndGet();
        }
        return refOID;
    }

    public abstract Address raw();

    /**
     * @return whether this instance is actually a local value dressed up to look like a remote reference
     */
    public boolean isLocal() {
        return false;
    }

    public boolean isLive() {
        return memoryStatus() == ObjectMemoryStatus.LIVE;
    }

    /**
     * Records that this instance refers to an object that has been copied elsewhere, and all
     * references should subsequently be forwarded.
     *
     * @param forwardedMutableTeleRef reference to another VM object that has superseded the one
     * to which this instance refers.
     */
    public final void setForwardedTeleReference(RemoteReference forwardedMutableTeleRef) {
        this.forwardedTeleRef = forwardedMutableTeleRef;
    }

    /**
     * @return reference to the VM object to which this instance refers, possibly following
     * a forwarding reference if set.
     */
    public final RemoteReference getForwardedTeleRef() {
        if (forwardedTeleRef != null) {
            return forwardedTeleRef.getForwardedTeleRef();
        }
        return this;
    }

    /**
     * @return the status of the memory to which this instance refers
     */
    public abstract ObjectMemoryStatus memoryStatus();

}
