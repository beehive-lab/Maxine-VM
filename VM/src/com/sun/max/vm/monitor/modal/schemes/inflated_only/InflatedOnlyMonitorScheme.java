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
