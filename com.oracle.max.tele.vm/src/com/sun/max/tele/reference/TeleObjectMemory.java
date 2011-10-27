/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.reference;

import java.util.*;

import com.sun.max.tele.object.*;

/**
 * The state of a VM object (or more precisely of a region in VM memory in which
 * object state is or was recorded).
 */
public interface TeleObjectMemory {

    public enum State {
        LIVE("Live", "The region of memory is in the live heap, represents an object that was reachable as of the most recent GC"),

        OBSOLETE("Obsolete", "The region of memory formerly represented an object, which has now been relocated by GC"),

        DEAD("Dead", "The region of memory formerly represented an object, which has now been collected by GC");

        private final String label;
        private final String description;

        private State(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String label() {
            return label;
        }

        public static final List<State> VALUES = Arrays.asList(values());
    }

    /**
     * The status of the object in the VM, or more precisely the status of the region of memory
     * in which this object's state is or was stored.  A new object is by definition {@linkplain TeleObjectMemory.State#LIVE "LIVE"}.
     * <br
     * If this object has been freed by the VM's garbage collector, it is no longer a legitimate object and the
     * state of this instance becomes {@linkplain TeleObjectMemory.State#DEAD "DEAD"} and does not change again.
     * <br>
     * If this object has been relocated by the VM's garbage collector, the memory referred to by this instance
     * is no longer a legitimate object and the state of this instance becomes {@linkplain TeleObjectMemory.State#OBSOLETE "OBSOLETE"}
     * and does not change again.
     * <br>
     * Once an object is no longer live, the  memory most recently allocated to it might be
     * reused by the VM.  For debugging purposes, however, a {@link TeleObject}
     * continues to refer to the abandoned memory as if it were an object.
     *
     * @return the state of the memory in which the object referred to by this instance is stored.
     * @see #isLive()
     * @see #isDead()
     * @see #isObsolete()
    */
    State getTeleObjectMemoryState();

    /**
     * Does the memory to which this instance refers contain a live
     * object in the VM?
     *
     * @return whether the object's memory in the VM is still live
     * @see #getTeleObjectMemoryState()
     */
    boolean isLive();

    /**
     * Does the memory to which this instance refers contain a copy
     * of an object in the VM that has been abandoned after relocation by GC.
     *
     * @return whether the object's memory in the VM has been relocated elsewhere.
     * @see #getTeleObjectMemoryState()
     */
    boolean isObsolete();

    /**
     * Does the memory to which this instance refers contain the state of
     * an object in the VM that is been released by GC.
     *
     * @return whether the object in the VM has been collected by GC.
     * @see #getTeleObjectMemoryState()
     */
    boolean isDead();

}
