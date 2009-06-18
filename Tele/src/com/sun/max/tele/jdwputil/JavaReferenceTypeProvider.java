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
package com.sun.max.tele.jdwputil;

import java.lang.reflect.*;

import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.vm.type.*;


abstract class JavaReferenceTypeProvider implements ReferenceTypeProvider {

    private Class _clazz;
    private ClassProvider _superClass;
    private ClassLoaderProvider _classLoader;
    private InterfaceProvider[] _implementedInterfaces;
    private MethodProvider[] _methodProviders;
    private VMAccess _vm;

    protected JavaReferenceTypeProvider(Class c, VMAccess vm, ClassLoaderProvider classLoader) {

        _clazz = c;
        _vm = vm;
        initReferencedClasses();
        initMethods();
    }

    public ClassProvider getSuperClass() {
        return _superClass;
    }

    private void initReferencedClasses() {

        if (_clazz.getSuperclass() != null) {
            final ReferenceTypeProvider referenceTypeProvider = _vm.getReferenceType(_clazz.getSuperclass());
            assert referenceTypeProvider instanceof ClassProvider;
            _superClass = (ClassProvider) referenceTypeProvider;
        }

        final Class[] interfaces = _clazz.getInterfaces();
        _implementedInterfaces = new InterfaceProvider[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            final ReferenceTypeProvider referenceTypeProvider = _vm.getReferenceType(interfaces[i]);
            assert referenceTypeProvider instanceof InterfaceProvider;
            _implementedInterfaces[i] = (InterfaceProvider) referenceTypeProvider;
        }

    }

    private void initMethods() {
        final Method[] methods = _clazz.getDeclaredMethods();
        _methodProviders = new MethodProvider[methods.length];
        for (int i = 0; i < methods.length; i++) {
            _methodProviders[i] = new JavaMethodProvider(methods[i], this, _vm);
        }
    }

    public ClassLoaderProvider classLoader() {
        return _classLoader;
    }

    public ClassObjectProvider classObject() {
        // TODO: Consider implementing otherwise.
        return null;
    }

    public FieldProvider[] getFields() {
        // Currently only methods are supported.
        return new FieldProvider[0];
    }

    public int getFlags() {
        return _clazz.getModifiers();
    }

    public InterfaceProvider[] getImplementedInterfaces() {
        return _implementedInterfaces;
    }

    public ObjectProvider[] getInstances() {
        // TODO: Consider implementing this otherwise.
        return new ObjectProvider[0];
    }

    public MethodProvider[] getMethods() {
        return _methodProviders;
    }

    public String getName() {
        return _clazz.getSimpleName();
    }

    public ReferenceTypeProvider[] getNestedTypes() {
        // Currently no nested types are supported.
        return new ReferenceTypeProvider[0];
    }

    public String getSignature() {
        return JavaTypeDescriptor.forJavaClass(_clazz).toString();
    }

    public String getSignatureWithGeneric() {
        return _clazz.getName();
    }

    public String getSourceFileName() {
        // Currently no source file name is supported.
        return "";
    }

    public int getStatus() {
        return ClassStatus.INITIALIZED;
    }

    public VMValue.Type getType() {
        if (this._clazz == Boolean.TYPE) {
            return VMValue.Type.BOOLEAN;
        } else if (this._clazz == Byte.TYPE) {
            return VMValue.Type.BYTE;
        } else if (this._clazz == Character.TYPE) {
            return VMValue.Type.CHAR;
        } else if (this._clazz == Double.TYPE) {
            return VMValue.Type.DOUBLE;
        } else if (this._clazz == Float.TYPE) {
            return VMValue.Type.FLOAT;
        } else if (this._clazz == Integer.TYPE) {
            return VMValue.Type.INT;
        } else if (this._clazz == Long.TYPE) {
            return VMValue.Type.LONG;
        } else if (this._clazz == Short.TYPE) {
            return VMValue.Type.SHORT;
        } else if (this._clazz == Void.TYPE) {
            return VMValue.Type.VOID;
        }

        return VMValue.Type.PROVIDER;
    }

    public int majorVersion() {
        // TODO: Check if this is correct.
        return 1;
    }

    public int minorVersion() {
        // TODO: Check if this is correct.
        return 5;
    }

    public ReferenceTypeProvider getReferenceType() {
        // TODO: Check if this is correct.
        return null;
    }
}
