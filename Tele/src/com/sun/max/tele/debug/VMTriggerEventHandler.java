/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

import com.sun.max.program.*;

/**
 * Handler for an event that triggers the VM to stop execution per
 * some request, for example a breakpoint or watchpoint.
 *
 * @author Michael Van De Vanter
 */
public interface VMTriggerEventHandler {

    /**
     * Perform any specific processing of a trigger event, for example a breakpoint
     * or watchpoint,  and decide
     * whether to stop VM execution or to resume execution silently. The default is
     * to stop VM execution.
     *
     * @param teleNativeThread the VM thread that triggered this event.
     * @return true if VM execution should really stop; false if VM execution should resume silently.
     */
    boolean handleTriggerEvent(TeleNativeThread teleNativeThread);

    public static final class Static {

        private static final int TRACE_VALUE = 1;

        private static final String tracePrefix = "[VMTriggerEventHandler] ";

        private Static() {
        }

        /**
         * A default handler for VM triggered VM events that always returns true.
         */
        public static VMTriggerEventHandler ALWAYS_TRUE = new VMTriggerEventHandler()      {
            public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                Trace.line(TRACE_VALUE, tracePrefix + "default handler, TRUE");
                return true;
            }
        };
    }
}
