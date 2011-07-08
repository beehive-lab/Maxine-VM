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
package com.sun.max.vm.classfile.constant;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.type.*;

/**
 * #4.4.2.
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
            result = ClassRegistry.OBJECT.findLocalVirtualMethodActor(name, descriptor);
            if (result != null) {
                return result;
            }
            return null;
        }

        static MethodActor resolve(ConstantPool pool, int index, ClassActor classActor, Utf8Constant name, SignatureDescriptor signature) {
            if (!classActor.isInterface()) {
                throw new IncompatibleClassChangeError();
            }
            final InterfaceActor interfaceActor = (InterfaceActor) classActor;
            MethodActor methodActor = findInterfaceMethodActor(interfaceActor, name, signature);
            if (methodActor != null) {
                MethodActor aliasedMethodActor = ALIAS.Static.aliasedMethod(methodActor);
                if (aliasedMethodActor == null) {
                    // Only update constant pool if no aliasing occurred.
                    // Otherwise, subsequent verification of bytecode
                    // referencing the alias method will fail.
                    pool.updateAt(index, new Resolved(methodActor));
                } else {
                    methodActor = aliasedMethodActor;
                }
                return methodActor;
            }
            final String errorMessage = classActor.javaSignature(true) + "." + name + signature;
            if (MaxineVM.isHosted()) {
                final Class<?> javaClass = classActor.toJava();
                final Class[] parameterTypes = signature.resolveParameterTypes(javaClass.getClassLoader());
                final Class returnType = signature.resolveReturnType(javaClass.getClassLoader());
                final Method method = Classes.resolveMethod(javaClass, returnType, name.string, parameterTypes);
                if (MaxineVM.isHostedOnly(method)) {
                    throw new HostOnlyMethodError(errorMessage);
                }
            }
            throw new NoSuchMethodError(errorMessage);
        }

        public MethodActor resolve(ConstantPool pool, int index) {
            return resolve(pool, index, holder, name, signature());
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
            final ClassActor classActor = pool.classAt(classIndex).resolve(pool, classIndex);
            return Unresolved.resolve(pool, index, classActor, name(pool), signature(pool));
        }

        @Override
        boolean isFieldConstant() {
            return false;
        }
    }
}
