/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.ignore;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 * The only purpose of this class is to recreate the early days when monitors were simply not implemented!
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
 */
public class IgnoreMonitorScheme extends AbstractMonitorScheme implements MonitorScheme {

    @HOSTED_ONLY
    public IgnoreMonitorScheme() {
        super();
    }

    @INLINE
    private Address hashCodeToMisc(int hashCode) {
        return Address.fromUnsignedInt(hashCode);
    }

    @INLINE
    private int miscToHashCode(Word misc) {
        return misc.asAddress().toInt();
    }

    public int makeHashCode(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isHosted()) {
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
        if (MaxineVM.isHosted()) {
            HostMonitor.enter(object);
        }
    }

    public final void monitorExit(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isHosted()) {
            HostMonitor.exit(object);
        }
    }

    public void monitorWait(Object object, long timeout)  throws InterruptedException {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isHosted()) {
            HostMonitor.wait(object, timeout);
        }
    }

    public void monitorNotify(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isHosted()) {
            HostMonitor.notify(object);
        }
    }

    public void monitorNotifyAll(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (MaxineVM.isHosted()) {
            HostMonitor.notifyAll(object);
        }
    }

    public Word createMisc(Object object) {
        return Address.fromUnsignedInt(createHashCode(object));
    }

    public void scanReferences(PointerIndexVisitor pointerIndexVisitor) {
    }

    public boolean threadHoldsMonitor(Object object, VmThread thread) {
        if (object == null) {
            throw new NullPointerException();
        }
        return false;
    }

    public void afterGarbageCollection() {
        // Not used.
    }

    public void beforeGarbageCollection() {
        // Not used.
    }
}
