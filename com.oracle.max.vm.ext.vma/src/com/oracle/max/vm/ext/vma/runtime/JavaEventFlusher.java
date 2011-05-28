/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.max.vm.ext.vma.runtime.JavaVMAdviceHandlerEvents.*;
import com.sun.max.vm.thread.*;

/**
 * The interface through which {@link JavaEventVMAdviceHandler} flushes the per-thread event buffer.
 */

public interface JavaEventFlusher {

    /**
     * Thread-specific buffer of events.
     */
    public static class EventBuffer {
        VmThread vmThread;
        Event[] events;
        int index;

        public EventBuffer(Event[] events) {
            this.events = events;
            this.vmThread = VmThread.current();
        }
    }

    /**
     * Flush the event buffer.
     * @param buffer
     */
    void flushBuffer(EventBuffer buffer);

    void initialise(ObjectStateHandler state);
    void finalise();

}
