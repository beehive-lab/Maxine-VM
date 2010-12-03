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
package com.sun.max.vm.runtime;

import static com.sun.max.platform.Platform.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
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
            final Class<?> trapStateAccessClass = Class.forName(MaxPackage.fromClass(TrapStateAccess.class).subPackage(isa.toLowerCase()).name()
                                                  + "." + isa + TrapStateAccess.class.getSimpleName());
            return (TrapStateAccess) trapStateAccessClass.newInstance();
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
