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
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;

/**
 * A native function represents a {@linkplain #makeSymbol() symbol} associated with a {@linkplain #classMethodActor()
 * method} that can be {@linkplain #link() linked} at runtime to produce a native machine code pointer.
 *
 * @author Doug Simon
 */
public class NativeFunction {
    private final ClassMethodActor classMethodActor;
    private String symbol;
    @CONSTANT_WHEN_NOT_ZERO
    private Word address = Address.zero();

    public NativeFunction(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
    }

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    /**
     * Determines if this method actor has the same name as any other native method actor defined in the same class.
     */
    @NEVER_INLINE
    private boolean isOverloadedByNativeMethod() {
        return classMethodActor.holder().forAllClassMethodActors(new Predicate<ClassMethodActor>() {
            public boolean evaluate(ClassMethodActor cma) {
                return classMethodActor != cma && cma.isNative() && cma.name.equals(classMethodActor.name);
            }
        });
    }

    /**
     * Gets the native symbol derived from the method this native function implements.
     *
     * Only the first call to this method can cause allocation. Subsequent calls return the value cached by the
     * first call.
     */
    public String makeSymbol() {
        if (symbol == null) {
            final ClassMethodActor m = classMethodActor;
            symbol = m.isCFunction() ? m.name.toString() : Mangle.mangleMethod(m.holder().typeDescriptor, m.name.toString(), isOverloadedByNativeMethod() ? m.descriptor() : null);
        }
        return symbol;
    }

    /**
     * Gets the native function pointer for this native function, linking it first if necessary.
     *
     * There's no need to synchronize this method - it's fine for multiple threads to each link the native function in
     * the case where's there is a race as the result of resolution will always be the same value.
     *
     * ATTENTION: do not declare this synchronized - it is called in the primordial phase.
     *
     * @throws UnsatisfiedLinkError if the native function cannot be found
     */
    public Word link() throws UnsatisfiedLinkError {
        if (address.isZero()) {
            address = DynamicLinker.lookup(classMethodActor, makeSymbol());
            if (!MaxineVM.isPrimordialOrPristine()) {
                if (NativeInterfaces.verbose()) {
                    Log.println("[Dynamic-linking native method " + classMethodActor.holder().name + "." + classMethodActor.name + " = " + address.toHexString() + "]");
                }
            }
        }
        return address;
    }

    /**
     * Determines if the native function pointer has been linked.
     */
    public boolean isLinked() {
        return !address.isZero();
    }

    /**
     * Sets (or clears) the machine code address for this native function.
     */
    public void setAddress(Word address) {
        this.address = address;
        if (!MaxineVM.isPrimordialOrPristine()) {
            if (NativeInterfaces.verbose()) {
                Log.println("[" + (address.isZero() ? "Unregistering" : "Registering") + " JNI native method " + classMethodActor.holder().name + "." + classMethodActor.name + "]");
            }
        }
    }
}
