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

import com.sun.max.annotate.*;
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
public interface FieldRefConstant extends PoolConstant<FieldRefConstant>, MemberRefConstant<FieldRefConstant> {

    public static interface FieldRefKey extends PoolConstantKey<FieldRefConstant> {

        TypeDescriptor holder();

        Utf8Constant name();

        TypeDescriptor type();

        public static final class Util {

            private Util() {
            }

            public static boolean equals(FieldRefKey key, Object other) {
                if (other instanceof FieldRefKey) {
                    final FieldRefKey otherKey = (FieldRefKey) other;
                    return key.holder().equals(otherKey.holder()) && key.name().equals(otherKey.name()) && key.type().equals(otherKey.type());
                }
                return false;
            }
            public static int hashCode(FieldRefKey key) {
                return key.holder().hashCode() ^ key.name().hashCode() ^ key.type().hashCode();
            }
        }
    }

    TypeDescriptor type(ConstantPool pool);

    FieldActor resolve(ConstantPool pool, int index);

    FieldRefKey key(final ConstantPool pool);

    public static final class Resolved extends AbstractPoolConstant<FieldRefConstant> implements FieldRefConstant, FieldRefKey {

        @INSPECTED
        private final FieldActor _fieldActor;

        public FieldActor fieldActor() {
            return _fieldActor;
        }

        public Resolved(FieldActor fieldActor) {
            _fieldActor = fieldActor;
        }

        @Override
        public Tag tag() {
            return Tag.FIELD_REF;
        }

        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            return true;
        }

        public boolean isResolved() {
            return true;
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            return _fieldActor;
        }

        public TypeDescriptor holder() {
            return _fieldActor.holder().typeDescriptor();
        }

        public Utf8Constant name() {
            return _fieldActor.name();
        }

        public TypeDescriptor type() {
            return _fieldActor.descriptor();
        }

        public Utf8Constant name(ConstantPool pool) {
            return name();
        }

        public TypeDescriptor holder(ConstantPool pool) {
            return holder();
        }

        public Descriptor descriptor(ConstantPool pool) {
            return type();
        }

        public TypeDescriptor type(ConstantPool pool) {
            return type();
        }

        @Override
        public boolean equals(Object object) {
            return FieldRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return FieldRefKey.Util.hashCode(this);
        }

        @Override
        public FieldRefKey key(ConstantPool pool) {
            return this;
        }

        public String valueString(ConstantPool pool) {
            return _fieldActor.descriptor().toJavaString(false) + " " + _fieldActor.holder().name() + _fieldActor.name();
        }
    }

    static final class Unresolved extends UnresolvedRef<FieldRefConstant> implements FieldRefConstant, FieldRefKey {

        Unresolved(ClassActor holder, Utf8Constant name, Descriptor descriptor) {
            super(holder, name, descriptor);
        }

        @Override
        public Tag tag() {
            return Tag.FIELD_REF;
        }

        static FieldActor resolve(ConstantPool pool, int index, ClassActor holder, Utf8Constant name, TypeDescriptor descriptor) {
            final FieldActor fieldActor = holder.findFieldActor(name, descriptor);
            if (fieldActor != null) {
                fieldActor.checkAccessBy(pool.holder());
                pool.updateAt(index, new Resolved(fieldActor));
                return fieldActor;
            }
            throw new NoSuchFieldError(descriptor + " " + holder.javaSignature(true) + "." + name);
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            return resolve(pool, index, _holder, _name, type());
        }

        @Override
        public FieldRefKey key(ConstantPool pool) {
            return this;
        }

        @Override
        public boolean equals(Object object) {
            return FieldRefKey.Util.equals(this, object);
        }

        @Override
        public int hashCode() {
            return FieldRefKey.Util.hashCode(this);
        }

        @Override
        boolean isFieldConstant() {
            return true;
        }
    }

    static final class UnresolvedIndices extends UnresolvedRefIndices<FieldRefConstant> implements FieldRefConstant {

        UnresolvedIndices(int classIndex, int nameAndTypeIndex, Tag[] tags) {
            super(classIndex, nameAndTypeIndex, tags);
        }

        @Override
        public Tag tag() {
            return Tag.FIELD_REF;
        }

        @Override
        public FieldRefKey key(final ConstantPool pool) {
            class Key extends RefKey implements FieldRefKey {
                Key() {
                    super(pool, UnresolvedIndices.this);
                }

                public final TypeDescriptor type() {
                    return UnresolvedIndices.this.type(pool);
                }

                @Override
                public boolean equals(Object object) {
                    return FieldRefKey.Util.equals(this, object);
                }

                @Override
                public int hashCode() {
                    return FieldRefKey.Util.hashCode(this);
                }
            }
            return new Key();
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            final ClassActor classActor = pool.classAt(_classIndex).resolve(pool, _classIndex);
            return Unresolved.resolve(pool, index, classActor, name(pool), type(pool));
        }

        @Override
        boolean isFieldConstant() {
            return true;
        }
    }
}
