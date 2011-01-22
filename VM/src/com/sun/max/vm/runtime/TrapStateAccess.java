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
package com.sun.max.vm.runtime;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;

/**
 * The trap state area is the frame of a call to {@link Stubs#trapStub}.
 * It contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs.
 *
 * There is a single {@linkplain MaxineVM#trapStateAccess instance} of this class
 * for the current platform. This object is used to access and/or modify a given
 * trap state area.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 * @author Mick Jordan
 */
public abstract class TrapStateAccess {

    @HOSTED_ONLY
    public static TrapStateAccess create() {
        try {
            final String isa = platform().isa.name();
            final Class<?> c = Class.forName(getPackageName(TrapStateAccess.class) + "." + isa.toLowerCase() + "." + isa + TrapStateAccess.class.getSimpleName());
            return (TrapStateAccess) c.newInstance();
        } catch (Exception exception) {
            throw FatalError.unexpected("could not create TrapStateAccess", exception);
        }
    }

    /**
     * Gets the program counter denoting instruction causing the trap.
     * This is also the address to which the trap handler will return
     * unless the handler unwinds to an exception handler in a method
     * other than the one that trapped.
     *
     * @param trapState the block of memory holding the trap state
     */
    public abstract Pointer getPC(Pointer trapState);

    /**
     * Updates the address to which the trap handler will return.
     *
     * @param trapState the block of memory holding the trap state
     */
    public abstract void setPC(Pointer trapState, Pointer value);

    /**
     * Gets the value of the stack pointer at the point of the trap.
     *
     * @param trapState the block of memory holding the trap state
     */
    public abstract Pointer getSP(Pointer trapState);

    /**
     * Gets the value of the frame pointer at the point of the trap.
     *
     * @param trapState the block of memory holding the trap state
     */
    public abstract Pointer getFP(Pointer trapState);

    /**
     * Gets the value of the safepoint last saved in a given trap state area.
     *
     * @param trapState the block of memory holding the trap state
     * @return the value of the safepoint latch at the time the trap denoted by {@code trapState} occurred
     */
    public abstract Pointer getSafepointLatch(Pointer trapState);

    /**
     * Sets the value that will be restored to the safepoint latch when the trap returns.
     *
     * @param trapState the block of memory holding the trap state
     */
    public abstract void setSafepointLatch(Pointer trapState, Pointer value);

    /**
     * Gets the value of the {@linkplain Trap.Number trap number} in a given trap state area.
     *
     * @param trapState the block of memory holding the trap state
     * @return the value of the trap number in {@code trapState}
     */
    public abstract int getTrapNumber(Pointer trapState);

    /**
     * Gets the address of a contiguous memory area holding the register state saved at a trap.
     * If the trap was at a safepoint, then this is the area to which the
     * register reference map for the safepoint applies.
     *
     * @param trapState the block of memory holding the trap state
     * @return the address of the saved register state within {@code trapState}
     */
    public abstract Pointer getRegisterState(Pointer trapState);

    /**
     * Sets the value of the {@linkplain Trap.Number trap number} in a given trap state area.
     *
     * @param trapState the block of memory holding the trap state
     * @param trapNumber the value to which the trap number in {@code trapState} will be set
     */
    public abstract void setTrapNumber(Pointer trapState, int trapNumber);

    /**
     * Prints the contents of a given trap state area to the VM {@linkplain Log#out log} stream.
     * This method assumes that the caller holds the {@linkplain Log#lock() lock} on the stream.
     *
     * @param trapState the block of memory holding the trap state
     */
    public abstract void logTrapState(Pointer trapState);
}
