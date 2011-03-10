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
package com.sun.max.tele;

import com.sun.max.tele.data.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * Canonical surrogate for a {@link VmThread} in the {@link TeleVM}.
 * The name of a thread can be changed dynamically in the {@link TeleVM}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public class TeleVmThread extends TeleTupleObject {

    private String name;

    // the most recent state when we checked the name reference
    private long lastRefreshedEpoch = 0;

    // the string representing the name of the thread the last time we checked the {@link TeleVM}.
    // assume that strings are immutable, so only re-read when the reference changes.
    private Reference nameReference;

    public TeleVmThread(TeleVM teleVM, Reference vmThreadReference) {
        super(teleVM, vmThreadReference);
    }

    public String name() {
        if (vm().teleProcess().epoch() > lastRefreshedEpoch) {
            try {
                final Reference nameReference = vm().teleFields().VmThread_name.readReference(reference());
                if (this.nameReference == null || !nameReference.equals(this.nameReference)) {
                    if (nameReference.isZero()) {
                        name = "*unset*";
                    } else {
                        // Assume strings in the {@link TeleVM} don't change, so we don't need to re-read
                        // if we've already seen the string (depends on canonical references).
                        this.nameReference = nameReference;
                        try {
                            name = vm().getString(this.nameReference);
                        } catch (InvalidReferenceException invalidReferenceExceptoin) {
                            name = "?";
                        }
                    }
                }
                lastRefreshedEpoch = vm().teleProcess().epoch();
            } catch (DataIOError dataIOError) {
                name = "?";
            }
        }
        return name;
    }

    /**
     * Returns the native thread associated with this Java thread.
     * <br>
     * Thread safe.
     * @return the thread associated with this Java thread.
     */
    public MaxThread maxThread() {
        for (MaxThread maxThread : vm().state().threads()) {
            if (maxThread.teleVmThread() == this) {
                return maxThread;
            }
        }
        return null;
    }

}
