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
package com.sun.max.vm.monitor.modal.schemes.observer_thin_inflated;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin.*;
import com.sun.max.vm.monitor.modal.modehandlers.observer.*;
import com.sun.max.vm.monitor.modal.modehandlers.observer.ObserverModeHandler.*;
import com.sun.max.vm.monitor.modal.schemes.*;

/**
 * A modal monitor scheme that transitions between thin locks and inflated monitors.
 * An extra pass-through 'Observer' mode is defined at the top of the lock-mode hierarchy,
 * which allows {@link MonitorObserver MonitorObservers} to be notified of monitor events.
 *
 * @author Simon Wilkinson
 */
public class ObserverThinInflatedMonitorScheme extends ModalMonitorScheme {
    public ObserverThinInflatedMonitorScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration, ObserverModeHandler.asFastPath(
                               ThinLockModeHandler.asObserverModeDelegate(
                               InflatedMonitorModeHandler.asThinLockDelegate())));

        attach(new GCTracingObserver());
    }

    @Override
    public ModalLockWordDecoder getModalLockWordDecoder() {
        return new ModalLockWordDecoder() {
            @Override
            public boolean isLockWordInMode(ModalLockWord64 modalLockWord, Class<? extends ModalLockWord64> mode) {
                if (mode == ThinLockWord64.class) {
                    return ThinLockWord64.isThinLockWord(modalLockWord);
                } else if (mode == InflatedMonitorLockWord64.class) {
                    return InflatedMonitorLockWord64.isInflatedMonitorLockWord(modalLockWord);
                }
                return false;
            }
        };
    }

    @PROTOTYPE_ONLY
    public void attach(MonitorObserver observer) {
        final ObserverModeHandler observerModeHandler = (ObserverModeHandler) entryHandler();
        observerModeHandler.attach(observer);
    }
}
