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
package com.sun.max.vm.thread;

import static com.sun.max.vm.thread.VmThread.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;

/**
 * A Maxine VM specific version of {@link ThreadLocal} that is much more efficient.
 */
public class ObjectThreadLocal<Type> extends VmThreadLocal {

    /**
     * A sentinel value to disambiguate from {@code null} as an uninitialized value.
     */
    private static final Object UNINITIALIZED = new Object();

    @HOSTED_ONLY
    private final ThreadLocal<Type> hosted = new ThreadLocal<Type>() {
        @Override
        protected Type initialValue() {
            return ObjectThreadLocal.this.initialValue();
        }
    };

    @HOSTED_ONLY
    public ObjectThreadLocal(String name, String description) {
        super(name, true, description);
    }

    @Override
    public void initialize() {
        store3(Reference.fromJava(UNINITIALIZED));
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public Type get() {
        if (MaxineVM.isHosted()) {
            return hosted.get();
        }
        Object value = loadRef(currentTLA()).toJava();
        if (value == UNINITIALIZED) {
            value = initialValue();
            store3(Reference.fromJava(value));
        }
        Class<Type> type = null;
        return Utils.cast(type, value);
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, then {@code null} is returned.
     *
     * @return the current thread's value of this thread-local
     */
    public Type getWithoutInitialization() {
        if (MaxineVM.isHosted()) {
            return hosted.get();
        }
        Object value = loadRef(currentTLA()).toJava();
        if (value == UNINITIALIZED) {
            return null;
        }
        Class<Type> type = null;
        return Utils.cast(type, value);
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(Type value) {
        if (MaxineVM.isHosted()) {
            hosted.set(value);
        } else {
            store3(Reference.fromJava(value));
        }
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the this method will not
     * be invoked for the thread.
     *
     * This implementation simply returns {@code null}.
     *
     * @return the initial value for this thread-local
     */
    protected Type initialValue() {
        return null;
    }
}
