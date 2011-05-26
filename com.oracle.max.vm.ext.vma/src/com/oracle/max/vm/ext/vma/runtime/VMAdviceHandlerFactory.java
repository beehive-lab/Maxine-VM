/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.runtime;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.vm.heap.*;

/**
 * Factory for controlling which subclass of {@link VMAdviceHandler} is used.
 *
 * The default choice is {@link SyncLogObjectTracker} but this
 * can be changed with the {@value TRACKERCLASS_PROPERTY} system property,
 * which should be the fully qualified name of the class.
 *
 * N.B. The tracker instance must be allocated in the boot heap or immortal memory
 * as it is accessed during a garbage collection.
 *
 */
public class VMAdviceHandlerFactory {
    public static final String ADVICE_HANDLER_PROPERTY = "max.vma.advicehandler";

    public static VMAdviceHandler create() {
        VMAdviceHandler result = null;
        final String adviceHandlerClass = System.getProperty(ADVICE_HANDLER_PROPERTY);
        try {
            Heap.enableImmortalMemoryAllocation();
            if (adviceHandlerClass == null) {
                result = new SyncLogVMAdviceHandler();
            } else {
                try {
                    result = (VMAdviceHandler) Class.forName(adviceHandlerClass).newInstance();
                } catch (Exception exception) {
                    System.err.println("Error instantiating " + adviceHandlerClass
                            + ": " + exception);
                }
            }
        } finally {
            Heap.disableImmortalMemoryAllocation();
        }
        return result;
    }

}
