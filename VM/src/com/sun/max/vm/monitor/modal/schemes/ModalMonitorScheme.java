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
package com.sun.max.vm.monitor.modal.schemes;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.AbstractModeHandler.*;
import com.sun.max.vm.monitor.modal.sync.*;
import com.sun.max.vm.thread.*;

/**
 * Base class for modal MonitorSchemes.
 *
 * Provides runtime entry point into the fastest-path mode.
 *
 *
 * @author Simon Wilkinson
 */
public abstract class ModalMonitorScheme extends AbstractMonitorScheme {

    private final MonitorSchemeEntry entryHandler;

    /**
     * Constructs a ModalMonitorScheme, setting the fastest-path mode
     * to entryHandler.
     * @param entryHandler the fastest-path locking mode handler
     */
    @HOSTED_ONLY
    public ModalMonitorScheme(MonitorSchemeEntry entryHandler) {
        this.entryHandler = entryHandler;

        // Inform all mode handlers about the current monitor scheme
        ModeHandler handler = entryHandler;
        while (handler != null) {
            handler.setMonitorScheme(this);
            handler = handler.delegate();
        }
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted()) {
            ProgramError.check(Word.width() == 64, "ModalMonitorScheme requires a 64-bit word.");
        }
        ModeHandler handler = entryHandler;
        while (handler != null) {
            handler.initialize(phase);
            handler = handler.delegate();
        }
        JavaMonitorManager.initialize(phase);
    }

    /**
     * Returns the fastest-path mode handler.
     *
     * @return the fastest-path mode handler
     */
    protected final MonitorSchemeEntry entryHandler() {
        return entryHandler;
    }

    /**
     * Inspector support for decoding lock words.
     *
     * @author Simon Wilkinson
     */
    public interface ModalLockwordDecoder {
        /**
         * Tests if the lockword is in the given mode.
         *
         * @param modalLockword the lock word to test
         * @param mode the mode to test
         * @return true if the lockword is in the given mode; false otherwise
         */
        boolean isLockwordInMode(ModalLockword64 modalLockword, Class<? extends ModalLockword64> mode);
    }

    /**
     * Returns a new {@code ModalLockwordDecoder} that can decode
     * lock words created by this {@code ModalMonitorScheme}.
     *
     * This should only be used for Inspector support.
     *
     * @return
     */
    public abstract ModalLockwordDecoder getModalLockwordDecoder();

    @INLINE
    public final Word createMisc(Object object) {
        return entryHandler.createMisc(object);
    }

    @INLINE
    public final int makeHashCode(Object object) {
        return entryHandler.makeHashCode(object);
    }

    @INLINE
    public final void monitorEnter(Object object) {
        entryHandler.monitorEnter(object);
    }

    @INLINE
    public final void monitorExit(Object object) {
        entryHandler.monitorExit(object);
    }

    @INLINE
    public final void monitorNotify(Object object) {
        entryHandler.monitorNotify(object, false);
    }

    @INLINE
    public final void monitorNotifyAll(Object object) {
        entryHandler.monitorNotify(object, true);
    }

    @INLINE
    public final void monitorWait(Object object, long timeout) throws InterruptedException {
        entryHandler.monitorWait(object, timeout);
    }

    @INLINE
    public final boolean threadHoldsMonitor(Object object, VmThread thread) {
        return entryHandler.threadHoldsMonitor(object, thread);
    }

    @INLINE
    public final void beforeGarbageCollection() {
        entryHandler.beforeGarbageCollection();
    }

    @INLINE
    public final void afterGarbageCollection() {
        entryHandler.afterGarbageCollection();
    }

    @INLINE
    public final void scanReferences(PointerIndexVisitor pointerIndexVisitor) {
        // We don't refer to any Java objects through pointers
    }

}
