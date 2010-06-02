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
package com.sun.max.tele.grip;

import java.util.*;

import com.sun.max.tele.object.*;

/**
 * The state of a VM object (or more precisely of a region in VM memory in which
 * object state is or was recorded).
 *
 * @author Hannes Payer
 * @author Michael Van De Vanter
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
