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
package com.sun.max.vm.monitor.modal.schemes;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
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

    private final MonitorSchemeEntry _entryHandler;

    /**
     * Constructs a ModalMonitorScheme, setting the fastest-path mode
     * to entryHandler.
     *
     * @param vmConfiguration the build-time VM configuration
     * @param entryHandler the fastest-path locking mode handler
     */
    public ModalMonitorScheme(VMConfiguration vmConfiguration, MonitorSchemeEntry entryHandler) {
        super(vmConfiguration);
        _entryHandler = entryHandler;

        // Inform all mode handlers about the current monitor scheme
        ModeHandler handler = _entryHandler;
        while (handler != null) {
            handler.setMonitorScheme(this);
            handler = handler.delegate();
        }
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PROTOTYPING || phase == MaxineVM.Phase.PRIMORDIAL) {
            ProgramError.check(Word.width() == WordWidth.BITS_64, "ModalMonitorScheme requires a 64-bit word.");
        }
        ModeHandler handler = _entryHandler;
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
        return _entryHandler;
    }

    /**
     * Inspector support for decoding lock words.
     *
     * @author Simon Wilkinson
     */
    public interface ModalLockWordDecoder {
        /**
         * Tests if the lockword is in the given mode.
         *
         * @param modalLockWord the lock word to test
         * @param mode the mode to test
         * @return true if the lockword is in the given mode; false otherwise
         */
        boolean isLockWordInMode(ModalLockWord64 modalLockWord, Class<? extends ModalLockWord64> mode);
    }

    /**
     * Returns a new <code>ModalLockWordDecoder</code> that can decode
     * lock words created by this <code>ModalMonitorScheme</code>.
     *
     * This should only be used for Inspector support.
     *
     * @return
     */
    public abstract ModalLockWordDecoder getModalLockWordDecoder();

    @Override
    @INLINE
    public final Word createMisc(Object object) {
        return _entryHandler.createMisc(object);
    }

    @Override
    @INLINE
    public final int makeHashCode(Object object) {
        return _entryHandler.makeHashCode(object);
    }

    @Override
    @INLINE
    public final void monitorEnter(Object object) {
        _entryHandler.monitorEnter(object);
    }

    @Override
    @INLINE
    public final void monitorExit(Object object) {
        _entryHandler.monitorExit(object);
    }

    @Override
    @INLINE
    public final void monitorNotify(Object object) {
        _entryHandler.monitorNotify(object, false);
    }

    @Override
    @INLINE
    public final void monitorNotifyAll(Object object) {
        _entryHandler.monitorNotify(object, true);
    }

    @Override
    @INLINE
    public final void monitorWait(Object object, long timeout) throws InterruptedException {
        _entryHandler.monitorWait(object, timeout);
    }

    @Override
    @INLINE
    public final boolean threadHoldsMonitor(Object object, VmThread thread) {
        return _entryHandler.threadHoldsMonitor(object, thread);
    }

    @Override
    @INLINE
    public final void beforeGarbageCollection() {
        _entryHandler.beforeGarbageCollection();
    }

    @Override
    @INLINE
    public final void afterGarbageCollection() {
        _entryHandler.afterGarbageCollection();
    }

    @Override
    @INLINE
    public final void scanReferences(PointerIndexVisitor pointerIndexVisitor) {
        // We don't refer to any Java objects through pointers
    }

}
