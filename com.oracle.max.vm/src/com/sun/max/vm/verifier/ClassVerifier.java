/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.verifier;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;

/**
 * An instance of {@code ClassVerifier} is created to verify the methods in a given class.
 */
public abstract class ClassVerifier extends Verifier {

    public final ClassActor classActor;
    protected ClassVerifier(ClassActor classActor) {
        super(classActor.constantPool());
        this.classActor = classActor;
    }

    /**
     * Performs bytecode verification for all methods in {@linkplain #classActor() the given class} that have a non-null
     * {@link ClassMethodActor#codeAttribute() code attribute}.
     */
    public synchronized void verify() {
        if (TraceVerifierLevel >= TRACE_CLASS) {
            Log.println("[Verifying class " + classActor.name + "]");
        }
        verifyMethods(classActor.localVirtualMethodActors());
        verifyMethods(classActor.localStaticMethodActors());
        verifyMethods(classActor.localInterfaceMethodActors());
        if (TraceVerifierLevel >= TRACE_CLASS) {
            Log.println("[Verified class " + classActor.name + "]");
        }
    }

    private void verifyMethods(MethodActor[] methodActors) {
        for (MethodActor methodActor : methodActors) {
            if (methodActor instanceof ClassMethodActor) {
                final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
                if (classMethodActor.compilee() == classMethodActor) {
                    verifyMethod(classMethodActor);
                } else {
                    // Cannot verify substituted methods as the receiver and holder type will not match
                }
            }
        }
    }

    protected void verifyMethod(final ClassMethodActor classMethodActor) {
        classMethodActor.verify(this);
    }

    /**
     * Performs bytecode verification on a given method.
     */
    public abstract CodeAttribute verify(ClassMethodActor classMethodActor, CodeAttribute codeAttribute);
}
