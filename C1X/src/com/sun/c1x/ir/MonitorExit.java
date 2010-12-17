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

/**
 * The {@code MonitorExit} instruction represents a monitor release.
 *
 * @author Ben L. Titzer
 */
public final class MonitorExit extends AccessMonitor {

    /**
     * Creates a new MonitorExit instruction.
     *
     * @param object the instruction produces the object value
     * @param lockAddress the address of the on-stack lock object or {@code null} if the runtime does not place locks on the stack
     * @param lockNumber the number of the lock
     * @param stateBefore the state before executing this instruction
     */
    public MonitorExit(Value object, Value lockAddress, int lockNumber, FrameState stateBefore) {
        super(object, lockAddress, stateBefore, lockNumber);
        if (object.isNonNull()) {
            eliminateNullCheck();
        }
    }

    @Override
    public boolean canTrap() {
        // C1X assumes that locks are well balanced and so there no need to handle
        // IllegalMonitorStateExceptions thrown by monitorexit instructions.
        return needsNullCheck();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitMonitorExit(this);
    }
}
