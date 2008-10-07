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
/*VCSID=9bf743b2-abc0-48f3-852f-43f91c94c948*/
package com.sun.max.vm.monitor.ignore;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.thread.*;

/**
 * The only purpose of this class is to recreate the early days when monitors were simply not implemented!
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
 */
public class IgnoreMonitorScheme extends AbstractMonitorScheme implements MonitorScheme {

    public IgnoreMonitorScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @INLINE
    private Address hashCodeToMisc(int hashCode) {
        return Address.fromInt(hashCode);
    }

    @INLINE
    private int miscToHashCode(Word misc) {
        return misc.asAddress().toInt();
    }

    public int makeHashCode(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isPrototyping()) {
            return createHashCode(object);
        }
        final Address oldMisc = ObjectAccess.readMisc(object).asAddress();
        final int oldResult = miscToHashCode(oldMisc);
        if (oldResult != 0) {
            return oldResult;
        }
        final int newResult = createHashCode(object);
        final Word newMisc = hashCodeToMisc(newResult);
        final Address answer = ObjectAccess.compareAndSwapMisc(object, Word.zero(), newMisc).asAddress();
        if (answer.isZero()) {
            return newResult;
        }
        return miscToHashCode(answer);
    }

    public final void monitorEnter(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isPrototyping()) {
            HostMonitor.enter(object);
        }
    }

    public final void monitorExit(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isPrototyping()) {
            HostMonitor.exit(object);
        }
    }

    public void monitorWait(Object object, long timeout)  throws InterruptedException {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isPrototyping()) {
            HostMonitor.wait(object, timeout);
        }
    }

    public void monitorNotify(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isPrototyping()) {
            HostMonitor.notify(object);
        }
    }

    public void monitorNotifyAll(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isPrototyping()) {
            HostMonitor.notifyAll(object);
        }
    }

    public Word createMisc(Object object) {
        return Address.fromInt(createHashCode(object));
    }

    @Override
    public void scanReferences(PointerIndexVisitor pointerIndexVisitor, RuntimeMemoryRegion from, RuntimeMemoryRegion to) {
    }

    @Override
    public boolean threadHoldsMonitor(Object object, VmThread thread) {
        if (object == null) {
            throw new NullPointerException();
        }
        return false;
    }

    @Override
    public void afterGarbageCollection() {
        // Not used.
    }

    @Override
    public void beforeGarbageCollection() {
        // Not used.
    }
}
