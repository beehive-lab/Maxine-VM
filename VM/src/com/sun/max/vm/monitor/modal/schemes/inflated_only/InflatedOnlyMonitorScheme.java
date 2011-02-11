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
package com.sun.max.vm.monitor.modal.schemes.inflated_only;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.schemes.*;
import com.sun.max.vm.monitor.modal.sync.*;

/**
 * A modal monitor scheme with no transitions: inflated only.
 * N.B. The default number of unbound monitors necessary to get the VM up
 * and running without excessive garbage collections is much higher than the
 * default in {@link JavaMonitorManager}. So we set it explicitly here unless
 * the user has already done so. The value was empirically determined.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
 */
public class InflatedOnlyMonitorScheme extends ModalMonitorScheme {
    @HOSTED_ONLY
    public InflatedOnlyMonitorScheme() {
        super(InflatedMonitorModeHandler.asFastPath());
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (MaxineVM.isHosted()) {
            final String qty = System.getProperty(JavaMonitorManager.UNBOUNDLIST_IMAGE_QTY_PROPERTY);
            if (qty == null) {
                System.setProperty(JavaMonitorManager.UNBOUNDLIST_IMAGE_QTY_PROPERTY, "2000");
            }
        }
        super.initialize(phase);
    }

    @Override
    public ModalLockwordDecoder getModalLockwordDecoder() {
        return new ModalLockwordDecoder() {
            public boolean isLockwordInMode(ModalLockword64 modalLockword, Class<? extends ModalLockword64> mode) {
                return mode == InflatedMonitorLockword64.class;
            }
        };
    }
}
