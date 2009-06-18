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
package com.sun.max.vm.classfile.constant;

import java.lang.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

/**
 * #4.4.2.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface InterfaceMethodRefConstant extends PoolConstant<InterfaceMethodRefConstant>, MethodRefConstant<InterfaceMethodRefConstant> {

    public static interface InterfaceMethodRefKey extends PoolConstantKey<InterfaceMethodRefConstant> {

        TypeDescriptor holder();

        Utf8Constant name();

        SignatureDescriptor signature();

        public static final class Util {

            private Util() {
            }

            public static boolean equals(InterfaceMethodRefKey key, Object other) {
                if (other instanceof InterfaceMethodRefKey) {
                    final InterfaceMethodRefKey otherKey = (InterfaceMethodRefKey) other;
                    return key.holder().equals(otherKey.holder()) && key.name().equals(otherKey.name()) && key.signature().equals(otherKey.signature());
                }
                return false;
            }
            public static int hashCode(InterfaceMethodRefKey key) {
                return key.holder().hashCode() ^ key.name().hashCode() ^ key.signature().hashCode();
            }
        }
    }

    InterfaceMethodRefKey key(final ConstantPool pool);

    static final class Resolved extends ResolvedMethodRefConstant<InterfaceMethodRefConstant> implements InterfaceMethodRefConstant, InterfaceMethodRefKey {

        public Resolved(MethodActor methodActor) {
            super(methodActor);
        }

        @Override
        public InterfaceMethodRefKey key(final ConstantPool pool) {
            return this;
        }

        @Override
        public Tag tag() {
            return Tag.INTERFACE_METHOD_REF;
        }

        @Override
        public boolean equals(Object object) {
            return InterfaceMethodRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return InterfaceMethodRefKey.Util.hashCode(this);
        }
    }

    static final class Unresolved extends UnresolvedRef<InterfaceMethodRefConstant> implements InterfaceMethodRefConstant, InterfaceMethodRefKey {

        public Unresolved(ClassActor holder, Utf8Constant name, SignatureDescriptor signature) {
            super(holder, name, signature);
        }

        @Override
        public Tag tag() {
            return Tag.INTERFACE_METHOD_REF;
        }

        @Override
        public InterfaceMethodRefKey key(final ConstantPool pool) {
            return this;
        }

        /**
         * Part of #5.4.3.4.
         */
        static MethodActor findInterfaceMethodActor(InterfaceActor interfaceActor, Utf8Constant name, SignatureDescriptor descriptor) {
            MethodActor result = interfaceActor.findLocalInterfaceMethodActor(name, descriptor);
            if (result != null) {
                return result;
            }
            for (InterfaceActor i : interfaceActor.localInterfaceActors()) {
                result = findInterfaceMethodActor(i, name, descriptor);
                if (result != null) {
                    return result;
                }
            }
            result = ClassRegistry.javaLangObjectActor().findLocalVirtualMethodActor(name, descriptor);
            if (result != null) {
                return result;
            }
            return null;
        }

        static MethodActor resolve(ConstantPool pool, int index, ClassActor classActor, Utf8Constant name, SignatureDescriptor signature) {
            if (!classActor.isInterfaceActor()) {
                throw new IncompatibleClassChangeError();
            }
            final InterfaceActor interfaceActor = (InterfaceActor) classActor;
            final MethodActor methodActor = findInterfaceMethodActor(interfaceActor, name, signature);
            if (methodActor != null) {
                pool.updateAt(index, new Resolved(methodActor));
                return methodActor;
            }
            final String errorMessage = classActor.javaSignature(true) + "." + name + signature;
            if (MaxineVM.isPrototyping()) {
                final Class<?> javaClass = classActor.toJava();
                final Class[] parameterTypes = signature.resolveParameterTypes(javaClass.getClassLoader());
                final Class returnType = signature.resolveReturnType(javaClass.getClassLoader());
                final Method method = Classes.resolveMethod(javaClass, returnType, name.string(), parameterTypes);
                if (MaxineVM.isPrototypeOnly(method)) {
                    throw new PrototypeOnlyMethodError(errorMessage);
                }
            }
            throw new NoSuchMethodError(errorMessage);
        }

        public MethodActor resolve(ConstantPool pool, int index) {
            return resolve(pool, index, _holder, _name, signature());
        }

        @Override
        public boolean equals(Object object) {
            return InterfaceMethodRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return InterfaceMethodRefKey.Util.hashCode(this);
        }

        @Override
        boolean isFieldConstant() {
            return false;
        }
    }

    static final class UnresolvedIndices extends UnresolvedRefIndices<InterfaceMethodRefConstant> implements InterfaceMethodRefConstant {

        UnresolvedIndices(int classIndex, int nameAndTypeIndex, Tag[] tags) {
            super(classIndex, nameAndTypeIndex, tags);
        }

        @Override
        public Tag tag() {
            return Tag.INTERFACE_METHOD_REF;
        }

        @Override
        public InterfaceMethodRefKey key(final ConstantPool pool) {
            class Key extends RefKey implements InterfaceMethodRefKey {
                Key() {
                    super(pool, UnresolvedIndices.this);
                }

                public final SignatureDescriptor signature() {
                    return UnresolvedIndices.this.signature(pool);
                }

                @Override
                public boolean equals(Object object) {
                    return InterfaceMethodRefKey.Util.equals(this, object);
                }

                @Override
                public int hashCode() {
                    return InterfaceMethodRefKey.Util.hashCode(this);
                }
            }
            return new Key();
        }

        public MethodActor resolve(ConstantPool pool, int index) {
            final ClassActor classActor = pool.classAt(_classIndex).resolve(pool, _classIndex);
            return Unresolved.resolve(pool, index, classActor, name(pool), signature(pool));
        }

        @Override
        boolean isFieldConstant() {
            return false;
        }
    }
}
