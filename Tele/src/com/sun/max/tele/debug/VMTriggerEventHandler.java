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
package com.sun.max.tele.debug;

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
        private Static() {
        }

        /**
         * A default handler for VM triggered VM events that always returns true.
         */
        public static VMTriggerEventHandler ALWAYS_TRUE = new VMTriggerEventHandler()      {
            public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
                return true;
            }
        };
    }
}
