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

import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.ValueType;

/**
 * The <code>NewInstance</code> instruction represents the allocation of an instance class object.
 *
 * @author Ben L. Titzer
 */
public class NewInstance extends StateSplit {

    CiType _instanceClass;

    /**
     * Constructs a NewInstance instruction.
     * @param theClass the class being allocated
     */
    public NewInstance(CiType theClass) {
        super(ValueType.OBJECT_TYPE);
        _instanceClass = theClass;
        setFlag(Flag.NonNull);
    }

    /**
     * Gets the instance class being allocatd by this instruction.
     * @return the instance class allocated
     */
    public CiType instanceClass() {
        return _instanceClass;
    }

    /**
     * Checks whether this instruction can trap.
     * @return <code>true</code>, assuming that allocation can cause OutOfMemory or other exceptions
     */
    public boolean canTrap() {
        return true;
    }

    /**
     * Gets the exact type produced by this instruction. For allocations of instance classes, this is
     * always the class allocated.
     * @return the exact type produced by this instruction
     */
    public CiType exactType() {
        return _instanceClass;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitNewInstance(this);
    }
}
