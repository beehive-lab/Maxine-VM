/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.schemes.observer_thin_inflated;

import com.sun.max.annotate.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin.*;
import com.sun.max.vm.monitor.modal.modehandlers.observer.*;
import com.sun.max.vm.monitor.modal.modehandlers.observer.ObserverModeHandler.MonitorObserver;
import com.sun.max.vm.monitor.modal.schemes.*;

/**
 * A modal monitor scheme that transitions between thin locks and inflated monitors.
 * An extra pass-through 'Observer' mode is defined at the top of the lock-mode hierarchy,
 * which allows {@link MonitorObserver MonitorObservers} to be notified of monitor events.
 */
public class ObserverThinInflatedMonitorScheme extends ModalMonitorScheme {
    @HOSTED_ONLY
    public ObserverThinInflatedMonitorScheme() {
        super(ObserverModeHandler.asFastPath(
                               ThinLockModeHandler.asObserverModeDelegate(
                               InflatedMonitorModeHandler.asThinLockDelegate())));

        attach(new GCTracingObserver());
    }

    @Override
    public ModalLockwordDecoder getModalLockwordDecoder() {
        return new ModalLockwordDecoder() {
            public boolean isLockwordInMode(ModalLockword64 modalLockword, Class<? extends ModalLockword64> mode) {
                if (mode == ThinLockword64.class) {
                    return ThinLockword64.isThinLockword(modalLockword);
                } else if (mode == InflatedMonitorLockword64.class) {
                    return InflatedMonitorLockword64.isInflatedMonitorLockword(modalLockword);
                }
                return false;
            }
        };
    }

    @HOSTED_ONLY
    public void attach(MonitorObserver observer) {
        final ObserverModeHandler observerModeHandler = (ObserverModeHandler) entryHandler();
        observerModeHandler.attach(observer);
    }
}
