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
package com.sun.max.vm.thread;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * A Maxine VM specific version of {@link ThreadLocal} that is much more efficient.
 *
 * @author Doug Simon
 */
public class ObjectThreadLocal<Type> extends VmThreadLocal {

    /**
     * A sentinel value to disambiguate from {@code null} as an uninitialized value.
     */
    private static final Object UNINITIALIZED = new Object();

    @PROTOTYPE_ONLY
    private final ThreadLocal<Type> prototype = new ThreadLocal<Type>() {
        @Override
        protected Type initialValue() {
            return ObjectThreadLocal.this.initialValue();
        }
    };

    @PROTOTYPE_ONLY
    public ObjectThreadLocal(String name, String description) {
        super(name, Kind.REFERENCE, description);
    }

    @Override
    public void initialize() {
        setVariableReference(Reference.fromJava(UNINITIALIZED));
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
        if (MaxineVM.isPrototyping()) {
            return prototype.get();
        }
        Object value = getVariableReference().toJava();
        if (value == UNINITIALIZED) {
            value = initialValue();
            setVariableReference(Reference.fromJava(value));
        }
        Class<Type> type = null;
        return StaticLoophole.cast(type, value);
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, then {@code null} is returned.
     *
     * @return the current thread's value of this thread-local
     */
    public Type getWithoutInitialization() {
        if (MaxineVM.isPrototyping()) {
            return prototype.get();
        }
        Object value = getVariableReference().toJava();
        if (value == UNINITIALIZED) {
            return null;
        }
        Class<Type> type = null;
        return StaticLoophole.cast(type, value);
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
        if (MaxineVM.isPrototyping()) {
            prototype.set(value);
        } else {
            setVariableReference(Reference.fromJava(value));
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
