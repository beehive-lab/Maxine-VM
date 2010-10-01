/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ir;

import com.sun.c1x.value.*;
import com.sun.cri.ci.*;

/**
 * The {@code AccessMonitor} instruction is the base class of both monitor acquisition and release.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessMonitor extends StateSplit {

    /**
     * The object locked or unlocked by this instruction.
     */
    Value object;

    private Value lockAddress;

    /**
     * The lock number of this monitor access.
     */
    public final int lockNumber;


    /**
     * Creates a new AccessMonitor instruction.
     * @param object the instruction producing the object
     * @param lockAddress the address of the on-stack lock object
     * @param stateBefore the state before executing the monitor operation
     * @param lockNumber the number of the lock being acquired
     */
    public AccessMonitor(Value object, Value lockAddress, FrameState stateBefore, int lockNumber) {
        super(CiKind.Illegal, stateBefore);
        this.object = object;
        this.lockAddress = lockAddress;
        this.lockNumber = lockNumber;
    }

    /**
     * Gets the instruction producing the object locked or unlocked by this instruction.
     */
    public Value object() {
        return object;
    }

    /**
     * Gets the instruction producing the address of the lock object.
     */
    public Value lockAddress() {
        return lockAddress;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        object = closure.apply(object);
        lockAddress = closure.apply(lockAddress);
    }

    @Override
    public boolean internalClearNullCheck() {
        return true;
    }
}
