/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
