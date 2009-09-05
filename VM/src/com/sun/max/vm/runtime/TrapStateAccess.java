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

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;

/**
 * The trap state area is a sub-section of the frame for a call to {@link Trap#trapStub}.
 * It contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs.
 *
 * There is a single {@linkplain #instance() instance} of this class
 * for the current platform. This object is used to access and/or modify a given
 * trap state area.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public abstract class TrapStateAccess {

    /**
     * Gets the platform specific instance of {@code TrapStateAccess}.
     */
    public static TrapStateAccess instance() {
        return VMConfiguration.host().trapStateAccess;
    }

    @PROTOTYPE_ONLY
    public static TrapStateAccess create(VMConfiguration vmConfiguration) {
        try {
            final String isa = vmConfiguration.platform().processorKind.instructionSet.name();
            final Class<?> trapStateAccessClass = Class.forName(MaxPackage.fromClass(TrapStateAccess.class).subPackage(isa.toLowerCase()).name()
                                                  + "." + isa + TrapStateAccess.class.getSimpleName());
            final Constructor<?> constructor = trapStateAccessClass.getConstructor(VMConfiguration.class);
            return (TrapStateAccess) constructor.newInstance(vmConfiguration);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw ProgramError.unexpected("could not create TrapStateAccess: " + exception);
        }
    }

    /**
     * Reads the value of the instruction pointer saved in a given trap state area.
     *
     * @param trapState the block of memory holding the trap state
     * @return the value of the instruction pointer saved in {@code trapState}
     */
    public abstract Pointer getInstructionPointer(Pointer trapState);

    /**
     * Sets the value that will be restored to the instruction pointer when the trap returns.
     *
     * @param trapState the block of memory holding the trap state
     * @param value the value to which the instruction pointer in {@code trapState} should be set
     */
    public abstract void setInstructionPointer(Pointer trapState, Pointer value);

    public abstract Pointer getStackPointer(Pointer trapState, TargetMethod targetMethod);
    public abstract Pointer getFramePointer(Pointer trapState, TargetMethod targetMethod);

    /**
     * Gets the value of the safepoint last saved in a given trap state area.
     *
     * @param trapState the block of memory holding the trap state
     * @return the value of the safepoint latch at the time the trap denoted by {@code trapState} occurred
     */
    public abstract Pointer getSafepointLatch(Pointer trapState);

    /**
     * Sets the value of the slot in a given trap state area corresponding to the
     * location in which a local exception handler expects to find the exception object.
     * A local exception handler is a handler that is in the same frame as the code
     * that trapped as a result of an implicit exception. On most platform, the location
     * used to communicate the exception object is the same register or memory location
     * used to return a reference value from a call (e.g. RAX on AMD64).
     *
     * @param trapState the block of memory holding the trap state
     * @param throwable the exception object that is being communicated to a local exception handler
     */
    public abstract void setExceptionObject(Pointer trapState, Throwable throwable);

    /**
     * Sets the value that will be restored to the safepoint latch when the trap returns.
     *
     * @param trapState the block of memory holding the trap state
     * @return the value of the safepoint latch at the time the trap denoted by {@code trapState} occurred
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
     * {@linkplain TargetMethod#registerReferenceMapFor(int) register reference map}
     * for the safepoint applies.
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
