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

    private Class _class;
    private ClassProvider _superClass;
    private ClassLoaderProvider _classLoader;
    private InterfaceProvider[] _implementedInterfaces;
    private MethodProvider[] _methodProviders;
    private VMAccess _vm;

    protected JavaReferenceTypeProvider(Class c, VMAccess vm, ClassLoaderProvider classLoader) {

        _class = c;
        _vm = vm;
        initReferencedClasses();
        initMethods();
    }

    public ClassProvider getSuperClass() {
        return _superClass;
    }

    private void initReferencedClasses() {

        if (_class.getSuperclass() != null) {
            final ReferenceTypeProvider referenceTypeProvider = _vm.getReferenceType(_class.getSuperclass());
            assert referenceTypeProvider instanceof ClassProvider;
            _superClass = (ClassProvider) referenceTypeProvider;
        }

        final Class[] interfaces = _class.getInterfaces();
        _implementedInterfaces = new InterfaceProvider[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            final ReferenceTypeProvider referenceTypeProvider = _vm.getReferenceType(interfaces[i]);
            assert referenceTypeProvider instanceof InterfaceProvider;
            _implementedInterfaces[i] = (InterfaceProvider) referenceTypeProvider;
        }

    }

    private void initMethods() {
        final Method[] methods = _class.getDeclaredMethods();
        _methodProviders = new MethodProvider[methods.length];
        for (int i = 0; i < methods.length; i++) {
            _methodProviders[i] = new JavaMethodProvider(methods[i], this, _vm);
        }
    }

    @Override
    public ClassLoaderProvider classLoader() {
        return _classLoader;
    }

    @Override
    public ClassObjectProvider classObject() {
        // TODO: Consider implementing otherwise.
        return null;
    }

    @Override
    public FieldProvider[] getFields() {
        // Currently only methods are supported.
        return new FieldProvider[0];
    }

    @Override
    public int getFlags() {
        return _class.getModifiers();
    }

    @Override
    public InterfaceProvider[] getImplementedInterfaces() {
        return _implementedInterfaces;
    }

    @Override
    public ObjectProvider[] getInstances() {
        // TODO: Consider implementing this otherwise.
        return new ObjectProvider[0];
    }

    @Override
    public MethodProvider[] getMethods() {
        return _methodProviders;
    }

    @Override
    public String getName() {
        return _class.getSimpleName();
    }

    @Override
    public ReferenceTypeProvider[] getNestedTypes() {
        // Currently no nested types are supported.
        return new ReferenceTypeProvider[0];
    }

    @Override
    public String getSignature() {
        return JavaTypeDescriptor.forJavaClass(_class).toString();
    }

    @Override
    public String getSignatureWithGeneric() {
        return _class.getName();
    }

    @Override
    public String getSourceFileName() {
        // Currently no source file name is supported.
        return "";
    }

    @Override
    public int getStatus() {
        return ClassStatus.INITIALIZED;
    }

    @Override
    public VMValue.Type getType() {

        if (this._class == Boolean.TYPE) {
            return VMValue.Type.BOOLEAN;
        } else if (this._class == Byte.TYPE) {
            return VMValue.Type.BYTE;
        } else if (this._class == Character.TYPE) {
            return VMValue.Type.CHAR;
        } else if (this._class == Double.TYPE) {
            return VMValue.Type.DOUBLE;
        } else if (this._class == Float.TYPE) {
            return VMValue.Type.FLOAT;
        } else if (this._class == Integer.TYPE) {
            return VMValue.Type.INT;
        } else if (this._class == Long.TYPE) {
            return VMValue.Type.LONG;
        } else if (this._class == Short.TYPE) {
            return VMValue.Type.SHORT;
        } else if (this._class == Void.TYPE) {
            return VMValue.Type.VOID;
        }

        return VMValue.Type.PROVIDER;
    }

    @Override
    public int majorVersion() {
        // TODO: Check if this is correct.
        return 1;
    }

    @Override
    public int minorVersion() {
        // TODO: Check if this is correct.
        return 5;
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        // TODO: Check if this is correct.
        return null;
    }
}
