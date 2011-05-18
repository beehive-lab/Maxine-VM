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

import static com.sun.max.vm.MaxineVM.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

import java.lang.reflect.*;

/**
 * #4.4.2.
 */
public interface ClassMethodRefConstant extends PoolConstant<ClassMethodRefConstant>, MethodRefConstant<ClassMethodRefConstant> {

    public static interface ClassMethodRefKey extends PoolConstantKey<ClassMethodRefConstant> {

        TypeDescriptor holder();

        Utf8Constant name();

        SignatureDescriptor signature();

        public static final class Util {

            private Util() {
            }

            public static boolean equals(ClassMethodRefKey key, Object other) {
                if (other instanceof ClassMethodRefKey) {
                    final ClassMethodRefKey otherKey = (ClassMethodRefKey) other;
                    return key.holder().equals(otherKey.holder()) && key.name().equals(otherKey.name()) && key.signature().equals(otherKey.signature());
                }
                return false;
            }
            public static int hashCode(ClassMethodRefKey key) {
                return key.holder().hashCode() ^ key.name().hashCode() ^ key.signature().hashCode();
            }
        }
    }

    StaticMethodActor resolveStatic(ConstantPool pool, int index);

    VirtualMethodActor resolveVirtual(ConstantPool pool, int index);

    ClassMethodRefKey key(final ConstantPool pool);

    static final class Resolved extends ResolvedMethodRefConstant<ClassMethodRefConstant> implements ClassMethodRefConstant, ClassMethodRefKey {

        public Resolved(MethodActor methodActor) {
            super(methodActor);
        }

        @Override
        public ClassMethodRefKey key(final ConstantPool pool) {
            return this;
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_REF;
        }

        @Override
        public boolean equals(Object object) {
            return ClassMethodRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return ClassMethodRefKey.Util.hashCode(this);
        }

        public StaticMethodActor resolveStatic(ConstantPool pool, int index) {
            return verifyIsStatic(methodActor(), pool);
        }

        public VirtualMethodActor resolveVirtual(ConstantPool pool, int index) {
            return verifyIsVirtual(methodActor(), pool);
        }
    }

    static final class Unresolved extends UnresolvedRef<ClassMethodRefConstant> implements ClassMethodRefConstant, ClassMethodRefKey {

        public Unresolved(ClassActor holder, Utf8Constant name, SignatureDescriptor signature) {
            super(holder, name, signature);
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_REF;
        }

        @Override
        public ClassMethodRefKey key(final ConstantPool pool) {
            return this;
        }

        static MethodActor resolve(ConstantPool pool, int index, ClassActor classActor, Utf8Constant name, SignatureDescriptor signature) {
            if (classActor.isInterface() && name != SymbolTable.CLINIT) {
                throw new IncompatibleClassChangeError();
            }
            // According to the JVM specification, we would have to look for interface methods,
            // but we do not need to do this here,
            // because we created Miranda methods for the TupleClassActor.
            // If we did not come across any of those above,
            // then there isn't any matching interface method either.
            MethodActor methodActor = classActor.findClassMethodActor(name, signature);
            if (methodActor != null) {
                if (methodActor.isAbstract() && !classActor.isAbstract()) {
                    throw new AbstractMethodError();
                }
                methodActor.checkAccessBy(pool.holder());
                if (isHosted()) {
                    MethodActor aliasedMethodActor = ALIAS.Static.resolveAlias(methodActor);
                    if (aliasedMethodActor == null) {
                        // Only update constant pool if no aliasing occurred.
                        // Otherwise, subsequent verification of bytecode
                        // referencing the alias method will fail.
                        pool.updateAt(index, new Resolved(methodActor));
                    } else {
                        methodActor = aliasedMethodActor;
                    }
                } else {
                    pool.updateAt(index, new Resolved(methodActor));
                }
                return methodActor;
            }
            final String errorMessage = classActor.javaSignature(true) + "." + name + signature;
            if (isHosted()) {
                final Class<?> javaClass = classActor.toJava();
                final Class[] parameterTypes = signature.resolveParameterTypes(javaClass.getClassLoader());
                final Class returnType = signature.resolveReturnType(javaClass.getClassLoader());
                final AccessibleObject member = name.equals(SymbolTable.INIT) ?
                    Classes.getDeclaredConstructor(javaClass, parameterTypes) :
                    Classes.resolveMethod(javaClass, returnType, name.string, parameterTypes);
                if (MaxineVM.isHostedOnly(member)) {
                    throw new HostOnlyMethodError(errorMessage);
                }
            }
            throw new NoSuchMethodError(errorMessage);
        }

        public MethodActor resolve(ConstantPool pool, int index) {
            return resolve(pool, index, holder, name, signature());
        }

        public StaticMethodActor resolveStatic(ConstantPool pool, int index) {
            return Resolved.verifyIsStatic(resolve(pool, index), pool);
        }

        public VirtualMethodActor resolveVirtual(ConstantPool pool, int index) {
            return Resolved.verifyIsVirtual(resolve(pool, index), pool);
        }

        @Override
        public boolean equals(Object object) {
            return ClassMethodRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return ClassMethodRefKey.Util.hashCode(this);
        }

        @Override
        boolean isFieldConstant() {
            return false;
        }
    }

    static final class UnresolvedIndices extends UnresolvedRefIndices<ClassMethodRefConstant> implements ClassMethodRefConstant {

        UnresolvedIndices(int classIndex, int nameAndTypeIndex, Tag[] tags) {
            super(classIndex, nameAndTypeIndex, tags);
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_REF;
        }

        @Override
        public ClassMethodRefKey key(final ConstantPool pool) {
            class Key extends RefKey implements ClassMethodRefKey {
                Key() {
                    super(pool, UnresolvedIndices.this);
                }

                public final SignatureDescriptor signature() {
                    return UnresolvedIndices.this.signature(pool);
                }

                @Override
                public boolean equals(Object object) {
                    return ClassMethodRefKey.Util.equals(this, object);
                }

                @Override
                public int hashCode() {
                    return ClassMethodRefKey.Util.hashCode(this);
                }
            }
            return new Key();
        }

        public MethodActor resolve(ConstantPool pool, int index) {
            final ClassActor classActor = pool.classAt(classIndex).resolve(pool, classIndex);
            return Unresolved.resolve(pool, index, classActor, name(pool), signature(pool));
        }

        public StaticMethodActor resolveStatic(ConstantPool pool, int index) {
            return Resolved.verifyIsStatic(resolve(pool, index), pool);
        }

        public VirtualMethodActor resolveVirtual(ConstantPool pool, int index) {
            return Resolved.verifyIsVirtual(resolve(pool, index), pool);
        }

        @Override
        boolean isFieldConstant() {
            return false;
        }
    }
}
