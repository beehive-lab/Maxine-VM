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
package com.sun.max.atomic;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;

/**
 * An object reference that may be updated atomically. See the {@link
 * java.util.concurrent.atomic} package specification for description
 * of the properties of atomic variables.
 *
 * @author Doug Lea (original JDK version)
 * @author Doug Simon
 */
public class AtomicReference {

    private static final int valueOffset = ClassActor.fromJava(AtomicReference.class).findLocalInstanceFieldActor("value").offset();

    private volatile Object value;

    /**
     * Creates a new AtomicReference with null initial value.
     */
    public AtomicReference() {
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    @INLINE
    public final Object get() {
        return value;
    }

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    @INLINE
    public final void set(Object newValue) {
        value = newValue;
    }

    /**
     * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that the actual value was not equal to the expected value.
     */
    @INLINE
    public final boolean compareAndSet(Object expect, Object update) {
        if (MaxineVM.isHosted()) {
            synchronized (this) {
                if (expect == value) {
                    value = update;
                    return true;
                }
                return false;
            }
        }
        final Reference expectReference = Reference.fromJava(expect);
        return Reference.fromJava(this).compareAndSwapReference(valueOffset, expectReference, Reference.fromJava(update)) == expectReference;
    }

    /**
     * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return the actual value compared against {@code expect}. The update is guaranteed to have occurred iff {@code expect != update} and
     * the returned value {@code == expect}
     */
    @INLINE
    public final Object compareAndSwap(Object expect, Object update) {
        if (MaxineVM.isHosted()) {
            synchronized (this) {
                if (expect == value) {
                    value = update;
                    return expect;
                }
                return value;
            }
        }
        return Reference.fromJava(this).compareAndSwapReference(valueOffset, Reference.fromJava(expect), Reference.fromJava(update)).toJava();
    }

    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    public final Object getAndSet(Object newValue) {
        while (true) {
            final Object currentValue = get();
            if (compareAndSet(currentValue, newValue)) {
                return currentValue;
            }
        }
    }

    /**
     * Returns the String representation of the current value.
     */
    @Override
    public String toString() {
        return String.valueOf(get());
    }
}
