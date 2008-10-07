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

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;

/**
 * A globally consistent, atomically updated flag.
 * 
 * @author Bernd Mathiske
 * @author Sunil Soman
 */
public class AtomicFlag {

    private Address _value;
    private final FieldActor _fieldActor = ClassActor.fromJava(AtomicFlag.class).findLocalInstanceFieldActor("_value");

    public AtomicFlag() {
    }

    /**
     * Atomically, visible in all threads, sets the flag.
     */
    public void set() {
        if (MaxineVM.isPrototyping()) {
            _value = Address.fromInt(1);
        } else {
            Reference.fromJava(this).compareAndSwapWord(_fieldActor.offset(), Address.zero(), Address.fromInt(1));
        }
    }

    /**
     * Atomically, clears the flag and returns whether it was clear.
     */
    public boolean clear() {
        if (MaxineVM.isPrototyping()) {
            final boolean result = _value.isZero();
            _value = Address.zero();
            return result;
        }
        return Reference.fromJava(this).compareAndSwapWord(_fieldActor.offset(), Address.fromInt(1), Address.zero()).isZero();
    }

}
