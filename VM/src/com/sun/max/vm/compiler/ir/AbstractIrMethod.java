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
package com.sun.max.vm.compiler.ir;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.jni.*;

/**
 * Abstract class used by most implementations of {@link IrMethod}.
 *
 * @author Doug Simon
 */
public abstract class AbstractIrMethod implements IrMethod {

    private final ClassMethodActor _classMethodActor;

    protected AbstractIrMethod(ClassMethodActor classMethodActor) {
        _classMethodActor = classMethodActor;
    }

    public ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    public String name() {
        return classMethodActor().name().toString();
    }

    public boolean isNative() {
        return classMethodActor().compilee().isNative();
    }

    public void cleanup() {
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return MethodID.fromMethodActor(_classMethodActor);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + _classMethodActor;
    }

    public boolean contains(final Builtin builtin, boolean defaultResult) {
        return defaultResult;
    }

    public int count(final Builtin builtin, int defaultResult) {
        return defaultResult;
    }

    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return IrTraceObserver.class;
    }
}
