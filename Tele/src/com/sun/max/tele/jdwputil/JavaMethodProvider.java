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
/*VCSID=bb8024cd-f886-4ef6-b6f1-b80dfc43884e*/
package com.sun.max.tele.jdwputil;

import java.lang.reflect.*;
import java.util.logging.*;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.vm.type.*;


class JavaMethodProvider implements MethodProvider {

    private static final Logger LOGGER = Logger.getLogger(JavaMethodProvider.class.getName());

    private Method _method;
    private ReferenceTypeProvider _holder;
    private VMAccess _vm;

    JavaMethodProvider(Method method, ReferenceTypeProvider holder, VMAccess vm) {
        _method = method;
        _holder = holder;
        _vm = vm;
    }

    @Override
    public int getFlags() {
        return _method.getModifiers();
    }

    @Override
    public LineTableEntry[] getLineTable() {
        return new LineTableEntry[0];
    }

    @Override
    public String getName() {
        // TODO: Check if this is correct.
        return _method.getName();
    }

    @Override
    public int getNumberOfParameters() {
        return _method.getParameterTypes().length;
    }

    @Override
    public ReferenceTypeProvider getReferenceTypeHolder() {
        return _holder;
    }

    @Override
    public String getSignature() {
        return SignatureDescriptor.fromJava(_method).toString();
    }

    @Override
    public String getSignatureWithGeneric() {
        // TODO: Check if this is correct.
        return getSignature();
    }

    @Override
    public VariableTableEntry[] getVariableTable() {
        return new VariableTableEntry[0];
    }

    @Override
    public VMValue invoke(ObjectProvider object, VMValue[] args, ThreadProvider threadProvider, boolean singleThreaded, boolean nonVirtual) {

        LOGGER.info("Method " + _method.getName() + " of class " + _method.getDeclaringClass() + " was invoked with arguments: " + args);
        final Object[] arguments = new Object[args.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = args[i].asJavaObject();
        }

        final VMValue objectValue = _vm.createObjectProviderValue(object);
        Object instanceObject = null;
        if (objectValue != null) {
            instanceObject = objectValue.asJavaObject();
        }

        Object result = null;
        try {
            _method.setAccessible(true);
            result = _method.invoke(instanceObject, arguments);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Exception while invoking method " + _method.getName(), e);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Exception while invoking method " + _method.getName(), e);
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Exception while invoking method " + _method.getName(), e);
        }

        if (_method.getReturnType() == Void.TYPE) {
            return _vm.getVoidValue();
        }


        return _vm.createJavaObjectValue(result, _method.getReturnType());
    }

    @Override
    public VMValue invokeStatic(VMValue[] args, ThreadProvider threadProvider, boolean singleThreaded) {
        return invoke(null, args, threadProvider, singleThreaded, false);
    }

    @Override
    public TargetMethodAccess[] getTargetMethods() {
        return new TargetMethodAccess[0];
    }
}
