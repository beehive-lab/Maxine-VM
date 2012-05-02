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
package com.sun.max.tele.jdwputil;

import java.lang.reflect.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.vm.type.*;

class JavaMethodProvider implements MethodProvider {

    private static final Logger LOGGER = Logger.getLogger(JavaMethodProvider.class.getName());

    private Method method;
    private ReferenceTypeProvider holder;
    private VMAccess vm;

    JavaMethodProvider(Method method, ReferenceTypeProvider holder, VMAccess vm) {
        this.method = method;
        this.holder = holder;
        this.vm = vm;
    }

    public int getFlags() {
        return method.getModifiers();
    }

    public LineTableEntry[] getLineTable() {
        return new LineTableEntry[0];
    }

    public String getName() {
        // TODO: Check if this is correct.
        return method.getName();
    }

    public int getNumberOfArguments() {
        int count = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
        for (Class type : method.getParameterTypes()) {
            if (type.equals(double.class) || type.equals(long.class)) {
                count += 2;
            } else {
                count++;
            }
        }
        return count;
    }

    public ReferenceTypeProvider getReferenceTypeHolder() {
        return holder;
    }

    public String getSignature() {
        return SignatureDescriptor.fromJava(method).toString();
    }

    public String getSignatureWithGeneric() {
        // TODO: Check if this is correct.
        return getSignature();
    }

    public VariableTableEntry[] getVariableTable() {
        return new VariableTableEntry[0];
    }

    public VMValue invoke(ObjectProvider object, VMValue[] args, ThreadProvider threadProvider, boolean singleThreaded, boolean nonVirtual) {

        LOGGER.info("Method " + method.getName() + " of class " + method.getDeclaringClass() + " was invoked with arguments: " + args);
        final Object[] arguments = new Object[args.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = args[i].asJavaObject();
        }

        final VMValue objectValue = vm.createObjectProviderValue(object);
        Object instanceObject = null;
        if (objectValue != null) {
            instanceObject = objectValue.asJavaObject();
        }

        Object result = null;
        try {
            method.setAccessible(true);
            result = method.invoke(instanceObject, arguments);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Exception while invoking method " + method.getName(), e);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Exception while invoking method " + method.getName(), e);
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Exception while invoking method " + method.getName(), e);
        }

        if (method.getReturnType() == Void.TYPE) {
            return vm.getVoidValue();
        }

        return vm.createJavaObjectValue(result, method.getReturnType());
    }

    public VMValue invokeStatic(VMValue[] args, ThreadProvider threadProvider, boolean singleThreaded) {
        return invoke(null, args, threadProvider, singleThreaded, false);
    }

    public TargetMethodAccess[] getTargetMethods() {
        return new TargetMethodAccess[0];
    }
}
