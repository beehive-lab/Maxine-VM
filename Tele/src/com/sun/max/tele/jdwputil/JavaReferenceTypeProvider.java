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

    private Class clazz;
    private ClassProvider superClass;
    private ClassLoaderProvider classLoader;
    private InterfaceProvider[] implementedInterfaces;
    private MethodProvider[] methodProviders;
    private VMAccess vm;

    protected JavaReferenceTypeProvider(Class c, VMAccess vm, ClassLoaderProvider classLoader) {
        this.clazz = c;
        this.vm = vm;
        initReferencedClasses();
        initMethods();
    }

    public ClassProvider getSuperClass() {
        return superClass;
    }

    private void initReferencedClasses() {

        if (clazz.getSuperclass() != null) {
            final ReferenceTypeProvider referenceTypeProvider = vm.getReferenceType(clazz.getSuperclass());
            assert referenceTypeProvider instanceof ClassProvider;
            superClass = (ClassProvider) referenceTypeProvider;
        }

        final Class[] interfaces = clazz.getInterfaces();
        implementedInterfaces = new InterfaceProvider[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            final ReferenceTypeProvider referenceTypeProvider = vm.getReferenceType(interfaces[i]);
            assert referenceTypeProvider instanceof InterfaceProvider;
            implementedInterfaces[i] = (InterfaceProvider) referenceTypeProvider;
        }

    }

    private void initMethods() {
        final Method[] methods = clazz.getDeclaredMethods();
        methodProviders = new MethodProvider[methods.length];
        for (int i = 0; i < methods.length; i++) {
            methodProviders[i] = new JavaMethodProvider(methods[i], this, vm);
        }
    }

    public ClassLoaderProvider classLoader() {
        return classLoader;
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
        return clazz.getModifiers();
    }

    public InterfaceProvider[] getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public ObjectProvider[] getInstances() {
        // TODO: Consider implementing this otherwise.
        return new ObjectProvider[0];
    }

    public MethodProvider[] getMethods() {
        return methodProviders;
    }

    public String getName() {
        return clazz.getSimpleName();
    }

    public ReferenceTypeProvider[] getNestedTypes() {
        // Currently no nested types are supported.
        return new ReferenceTypeProvider[0];
    }

    public String getSignature() {
        return JavaTypeDescriptor.forJavaClass(clazz).toString();
    }

    public String getSignatureWithGeneric() {
        return clazz.getName();
    }

    public String getSourceFileName() {
        // Currently no source file name is supported.
        return "";
    }

    public int getStatus() {
        return ClassStatus.INITIALIZED;
    }

    public VMValue.Type getType() {
        if (this.clazz == Boolean.TYPE) {
            return VMValue.Type.BOOLEAN;
        } else if (this.clazz == Byte.TYPE) {
            return VMValue.Type.BYTE;
        } else if (this.clazz == Character.TYPE) {
            return VMValue.Type.CHAR;
        } else if (this.clazz == Double.TYPE) {
            return VMValue.Type.DOUBLE;
        } else if (this.clazz == Float.TYPE) {
            return VMValue.Type.FLOAT;
        } else if (this.clazz == Integer.TYPE) {
            return VMValue.Type.INT;
        } else if (this.clazz == Long.TYPE) {
            return VMValue.Type.LONG;
        } else if (this.clazz == Short.TYPE) {
            return VMValue.Type.SHORT;
        } else if (this.clazz == Void.TYPE) {
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
