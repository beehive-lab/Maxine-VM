/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.jni.*;

/**
 * Abstract class used by most implementations of {@link IrMethod}.
 *
 * @author Doug Simon
 */
public abstract class AbstractIrMethod implements IrMethod {

    private final ClassMethodActor classMethodActor;

    protected AbstractIrMethod(ClassMethodActor classMethodActor) {
        this.classMethodActor = classMethodActor;
    }

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    public String name() {
        return classMethodActor().name.toString();
    }

    public boolean isNative() {
        return classMethodActor().compilee().isNative();
    }

    public void cleanup() {
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return MethodID.fromMethodActor(classMethodActor);
    }

    @Override
    public String toString() {
        return classMethodActor.format("%H.%n(%p)");
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
