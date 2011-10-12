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

    public static final class Resolved extends AbstractMemberRefConstant<FieldRefConstant> implements FieldRefConstant, FieldRefKey {

        @INSPECTED
        private final FieldActor fieldActor;

        public FieldActor fieldActor() {
            return fieldActor;
        }

        public Resolved(FieldActor fieldActor) {
            this.fieldActor = fieldActor;
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
            return fieldActor;
        }

        public TypeDescriptor holder() {
            return fieldActor.holder().typeDescriptor;
        }

        public Utf8Constant name() {
            return fieldActor.name;
        }

        public TypeDescriptor type() {
            return fieldActor.descriptor();
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
            return fieldActor.format("%H.%n:%t");
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

        static FieldActor resolve(ConstantPool pool, int index, ClassActor holder, Utf8Constant name, TypeDescriptor type) {
            FieldActor fieldActor = holder.findFieldActor(name, type);
            if (fieldActor != null) {
                fieldActor.checkAccessBy(pool.holder());
                FieldActor aliasedFieldActor = ALIAS.Static.aliasedField(fieldActor);
                if (aliasedFieldActor == null) {
                    // Only update constant pool if no aliasing occurred.
                    // Otherwise, subsequent verification of bytecode
                    // referencing the alias field will fail.
                    pool.updateAt(index, new Resolved(fieldActor));
                } else {
                    fieldActor = aliasedFieldActor;
                }
                return fieldActor;
            }
            final String errorMessage = type + " " + holder.javaSignature(true) + "." + name;
            if (MaxineVM.isHosted()) {
                final Class<?> javaClass = holder.toJava();
                final Class fieldType = type.resolveType(javaClass.getClassLoader());
                final Field field = Classes.resolveField(javaClass, fieldType, name.string);
                if (MaxineVM.isHostedOnly(field)) {
                    throw new HostOnlyFieldError(errorMessage);
                }
            }

            throw new NoSuchFieldError(errorMessage);
        }

        public FieldActor resolve(ConstantPool pool, int index) {
            return resolve(pool, index, holder, name, type());
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
            final ClassActor classActor = pool.classAt(classIndex).resolve(pool, classIndex);
            return Unresolved.resolve(pool, index, classActor, name(pool), type(pool));
        }

        @Override
        boolean isFieldConstant() {
            return true;
        }
    }
}
