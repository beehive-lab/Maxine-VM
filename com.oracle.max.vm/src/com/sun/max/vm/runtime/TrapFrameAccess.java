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
package com.sun.max.vm.runtime;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Accessor for the contents of the {@linkplain Stubs#trapStub trap stub's} frame.
 * It contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs.
 * <p>
 * There is a single {@linkplain MaxineVM#trapFrameAccess instance} of this class
 * for the current platform. This object is used to access and/or modify a given
 * trap frame.
 */
public abstract class TrapFrameAccess {

    @HOSTED_ONLY
    public static TrapFrameAccess create() {
        try {
            final String isa = platform().isa.name();
            final Class<?> c = Class.forName(getPackageName(TrapFrameAccess.class) + "." + isa.toLowerCase() + "." + isa + TrapFrameAccess.class.getSimpleName());
            return (TrapFrameAccess) c.newInstance();
        } catch (Exception exception) {
            throw FatalError.unexpected("could not create trapFrameAccess", exception);
        }
    }

    /**
     * Gets the address of the memory word in a given trap state holding
     * the program counter denoting instruction causing the trap.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public abstract Pointer getPCPointer(Pointer trapFrame);

    /**
     * Gets the program counter denoting instruction causing the trap.
     * This is also the address to which the trap handler will return
     * unless the handler unwinds to an exception handler in a method
     * other than the one that trapped.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public final Pointer getPC(Pointer trapFrame) {
        return getPCPointer(trapFrame).readWord(0).asPointer();
    }

    /**
     * Updates the address to which the trap handler will return.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public final void setPC(Pointer trapFrame, Pointer value) {
        getPCPointer(trapFrame).writeWord(0, value);
    }

    /**
     * Gets the value of the stack pointer at the point of the trap.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public abstract Pointer getSP(Pointer trapFrame);

    /**
     * Gets the value of the frame pointer at the point of the trap.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public abstract Pointer getFP(Pointer trapFrame);

    /**
     * Gets the value of the safepoint last saved in a given trap frame.
     *
     * @param trapFrame the block of memory holding the trap state
     * @return the value of the safepoint latch at the time the trap denoted by {@code trapFrame} occurred
     */
    public abstract Pointer getSafepointLatch(Pointer trapFrame);

    /**
     * Sets the value that will be restored to the safepoint latch when the trap returns.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public abstract void setSafepointLatch(Pointer trapFrame, Pointer value);

    /**
     * Gets the value of the {@linkplain Trap.Number trap number} in a given trap frame.
     *
     * @param trapFrame the block of memory holding the trap state
     * @return the value of the trap number in {@code trapFrame}
     */
    public abstract int getTrapNumber(Pointer trapFrame);

    /**
     * Gets the address of the callee save area in the trap stub's frame.
     *
     * @param trapFrame the trap stub's frame
     * @return the address of the callee save area within {@code trapFrame}
     */
    public abstract Pointer getCalleeSaveArea(Pointer trapFrame);

    /**
     * Sets the value of the {@linkplain Trap.Number trap number} in a given trap frame.
     *
     * @param trapFrame the block of memory holding the trap state
     * @param trapNumber the value to which the trap number in {@code trapFrame} will be set
     */
    public abstract void setTrapNumber(Pointer trapFrame, int trapNumber);

    /**
     * Prints the contents of a given trap frame to the VM {@linkplain Log#out log} stream.
     * This method assumes that the caller holds the {@linkplain Log#lock() lock} on the stream.
     *
     * @param trapFrame the block of memory holding the trap state
     */
    public abstract void logTrapFrame(Pointer trapFrame);
}
