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
package com.sun.max.vm.monitor.modal.schemes.biased_inflated;

import com.sun.max.vm.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.monitor.modal.schemes.*;

/**
 * A modal monitor scheme that transitions between biased locks and inflated monitors.
 *
 * @author Simon Wilkinson
 */
public class BiasedInflatedMonitorScheme extends ModalMonitorScheme {
    public BiasedInflatedMonitorScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration, BiasedLockModeHandler.asFastPath(false,
                               InflatedMonitorModeHandler.asBiasedLockDelegate()));
    }

    public ModalLockWordDecoder getModalLockWordDecoder() {
        return new ModalLockWordDecoder() {
            public boolean isLockWordInMode(ModalLockWord64 modalLockWord, Class<? extends ModalLockWord64> mode) {
                if (mode == BiasedLockWord64.class) {
                    return BiasedLockWord64.isBiasedLockWord(modalLockWord);
                } else if (mode == InflatedMonitorLockWord64.class) {
                    return InflatedMonitorLockWord64.isInflatedMonitorLockWord(modalLockWord);
                }
                return false;
            }

        };
    }
}
