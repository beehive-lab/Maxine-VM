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

import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

import java.lang.reflect.*;

/**
 * #4.4.2.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
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
            if (classActor.isInterfaceActor()) {
                throw new IncompatibleClassChangeError();
            }
            // According to the JVM specification, we would have to look for interface methods,
            // but we do not need to do this here,
            // because we created Miranda methods for the TupleClassActor.
            // If we did not come across any of those above,
            // then there isn't any matching interface method either.
            final MethodActor classMethodActor = classActor.findClassMethodActor(name, signature);
            if (classMethodActor != null) {
                if (classMethodActor.isAbstract() && !classActor.isAbstract()) {
                    throw new AbstractMethodError();
                }

                classMethodActor.checkAccessBy(pool.holder());
                pool.updateAt(index, new Resolved(classMethodActor));
                return classMethodActor;
            }
            final String errorMessage = classActor.javaSignature(true) + "." + name + signature;
            if (MaxineVM.isHosted()) {
                final Class<?> javaClass = classActor.toJava();
                final Class[] parameterTypes = signature.resolveParameterTypes(javaClass.getClassLoader());
                final Class returnType = signature.resolveReturnType(javaClass.getClassLoader());
                final AccessibleObject member = name.equals(SymbolTable.INIT) ?
                    Classes.getDeclaredConstructor(javaClass, parameterTypes) :
                    Classes.resolveMethod(javaClass, returnType, name.string, parameterTypes);
                if (MaxineVM.isPrototypeOnly(member)) {
                    throw new PrototypeOnlyMethodError(errorMessage);
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
