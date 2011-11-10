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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.interpreter.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A {@code TeleMethodAccess} provides a mechanism for accessing a method in a VM.
 * It includes support for {@linkplain #interpret(Value...) invoking} such a method in the
 * context of the VM.
 */
public abstract class TeleMethodAccess extends AbstractVmHolder {

    private static MethodActor findMethodActor(Class holder, String name, SignatureDescriptor signature) {
        final ClassActor classActor = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(holder));
        return classActor.findMethodActor(SymbolTable.makeSymbol(name), signature);
    }

    private static MethodActor findMethodActor(Class holder, String name) {
        final ClassActor classActor = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(holder));
        MethodActor uniqueMethodActor = null;
        for (MethodActor methodActor : classActor.getLocalMethodActors()) {
            if (methodActor.name.string.equals(name)) {
                if (uniqueMethodActor != null) {
                    TeleError.unexpected("need to disambiguate method named '" + name + "' in " + classActor.name + " with a signature");
                }
                uniqueMethodActor = methodActor;
            }
        }
        return uniqueMethodActor;
    }

    private final MethodActor methodActor;

    protected TeleMethodAccess(TeleVM vm, Class holder, String name, SignatureDescriptor signature) {
        super(vm);
        if (signature != null) {
            methodActor = findMethodActor(holder, name, signature);
            TeleError.check(methodActor != null, "could not find method " + name + signature + " in " + holder);
        } else {
            methodActor = findMethodActor(holder, name);
            TeleError.check(methodActor != null, "could not find method named '" + name + "' in " + holder);
        }
    }

    /**
     * @return the local descriptor for this method in the VM.
     */
    protected final MethodActor methodActor() {
        return methodActor;
    }

    /**
     * @return surrogate for the descriptor for this method in the VM.
     */
    public TeleClassMethodActor teleClassMethodActor() {
        return vm().findTeleMethodActor(TeleClassMethodActor.class, methodActor);
    }

    /**
     * Executes the method as if running in the VM.  Interprets (slowly) the bytecodes in an environment
     * where memory reads are remote to the VM, and in which the IDs of classes are those of the VM.
     * Local objects passed as arguments must be of types known as legitimate in the VM.
     *
     * @param arguments
     * @return return value from the method
     * @throws MaxVMBusyException if the VM is running and the interpreter cannot be used
     * @throws TeleInterpreterException if an uncaught exception occurs during execution of the method
     */
    public Value interpret(Value... arguments) throws MaxVMBusyException, TeleInterpreterException {
        TeleError.check(methodActor instanceof ClassMethodActor, "cannot interpret interface method");
        Value result = null;
        if (!vm().tryLock()) {
            throw new MaxVMBusyException();
        }
        try {
            result = TeleInterpreter.execute(vm(), (ClassMethodActor) methodActor, arguments);
        } finally {
            vm().unlock();
        }
        return result;
    }
}
