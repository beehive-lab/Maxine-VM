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

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;

import java.io.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.type.*;

/**
 * Constant pool constants, see #4.4.
 *
 * Each constant type defined by one of the {@linkplain Tag tags} in <a
 * href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#87125">Table 4.3</a> is represented
 * by a subclass of {@code PoolConstant}. There is exactly one subclass representing the final or resolved version of a
 * constant type. For some constant types, there are one or more additional subclasses representing unresolved
 * version(s) of the constant. The process of resolving a constant will always result with relevant constant pool slot
 * being updated with the resolved version. That is, the constant pool will never be
 * {@linkplain ConstantPool#updateAt(int, PoolConstant) updated} with an unresolved version of a constant.
 */
public interface PoolConstant<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> {

    Tag tag();

    PoolConstantKey<PoolConstant_Type> key(ConstantPool pool);

    /**
     * Gets a string description of this constant that uses a given pool reference to construct a more human readable
     * form of constants that cross-reference other pool constants.
     */
    String toString(ConstantPool pool);

    /**
     * Gets a string description of the value denoted by this constant that uses a given pool reference to construct a
     * more human readable form of values for constants that cross-reference other pool constants.
     *
     * @param pool
     *            a constant pool that can be used to get human readable strings for other pool constants
     *            referenced by this constant. This parameter may be null.
     */
    String valueString(ConstantPool pool);

    /**
     * Gets a string description of this constant. This version may not be so human
     * readable in the case of a constant that refers to other constants via unresolved
     * constant pool indexes.
     *
     * @see #toString(ConstantPool)
     */
    String toString();

    public static final class Static {

        private Static() {
        }

        public static String toString(PoolConstant poolConstant, ConstantPool pool) {
            String name = poolConstant.getClass().getName();
            name = name.substring(name.lastIndexOf('.') + 1); // strip the package name
            return "<" + name + "[" + poolConstant.valueString(pool) + "]>";
        }

        /**
         * Gets the constant pool index of a method or field's holder.
         *
         * @param memberRef a field or method reference
         * @return the index of {@code memberRef}'s holder entry in the same constant pool
         *         or -1 if the index is not available
         */
        public static int holderIndex(MemberRefConstant memberRef) {
            if (memberRef instanceof UnresolvedRefIndices) {
                UnresolvedRefIndices indices = (UnresolvedRefIndices) memberRef;
                return indices.classIndex;
            }
            return -1;
        }
    }

    /**
     * Writes myself to a class file stream.
     * This may cause extra entries to be added to the pool.
     *
     * @param stream stream to write to
     * @param editor constant pool editor currently writing
     * @param index index of ourselves in the editors pool
     */
    void writeOn(DataOutputStream stream, ConstantPoolEditor editor, int index) throws IOException;

}

//The rest of this file contains package-private abstract classes that provide most of the implementation
//for the pool constant interfaces.

/**
 * An abstract class that implements some of the functionality of a method or field entry in a constant pool
 * that is in a completely unresolved state. That is, the references to the holder, name and signature of
 * the field or method are indices to other constant pool entries.
 */
abstract class UnresolvedRefIndices<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends AbstractMemberRefConstant<PoolConstant_Type> {

    public final int classIndex;

    public final int nameAndTypeIndex;

    UnresolvedRefIndices(int classIndex, int nameAndTypeIndex, Tag[] tags) {
        this.classIndex       = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
        if (tags[classIndex] != CLASS) {
            throw ConstantPool.unexpectedEntry(classIndex, tags[classIndex], "defining class of field/method", CLASS);
        }
        if (tags[nameAndTypeIndex] != NAME_AND_TYPE) {
            throw ConstantPool.unexpectedEntry(nameAndTypeIndex, tags[nameAndTypeIndex], "field/method name and type", NAME_AND_TYPE);
        }
    }

    public final TypeDescriptor holder(ConstantPool pool) {
        final ClassConstant classRef = pool.classAt(classIndex);
        return classRef.typeDescriptor();
    }

    public final Utf8Constant name(ConstantPool pool) {
        return nameAndType(pool).name();
    }

    public final Descriptor descriptor(ConstantPool pool) {
        return nameAndType(pool).descriptor();
    }

    public final SignatureDescriptor signature(ConstantPool pool) {
        return nameAndType(pool).signature();
    }

    public final TypeDescriptor type(ConstantPool pool) {
        return nameAndType(pool).type();
    }

    public boolean isResolved() {
        return false;
    }

    @Override
    public final boolean isResolvableWithoutClassLoading(ConstantPool pool) {
        if (!pool.classAt(classIndex).isResolvableWithoutClassLoading(pool)) {
            return false;
        }
        if (isFieldConstant()) {
            return type(pool).isResolvableWithoutClassLoading(pool.classLoader());
        } else {
            SignatureDescriptor signature = signature(pool);
            for (int i = 0; i < signature.numberOfParameters(); i++) {
                if (!signature.parameterDescriptorAt(i).isResolvableWithoutClassLoading(pool.classLoader())) {
                    return false;
                }
            }
            return signature.resultDescriptor().isResolvableWithoutClassLoading(pool.classLoader());
        }
    }

    final NameAndTypeConstant nameAndType(ConstantPool pool) {
        return pool.nameAndTypeAt(nameAndTypeIndex);
    }

    abstract boolean isFieldConstant();

    public final String valueString(ConstantPool pool) {
        if (pool == null) {
            return "classIndex=" + classIndex + ",nameAndTypeIndex=" + nameAndTypeIndex;
        }
        if (isFieldConstant()) {
            return holder(pool).toJavaString(true) + '.' + name(pool) + ':' + type(pool).toJavaString(false);
        }
        final SignatureDescriptor signature = signature(pool);
        return holder(pool).toJavaString(true) + "." + name(pool) + signature.toJavaString(false, false) + ":" + signature.resultDescriptor().toJavaString(false);
    }
}

abstract class RefKey {

    final TypeDescriptor holder;
    final Utf8Constant name;

    RefKey(ConstantPool pool, UnresolvedRefIndices unresolvedRef) {
        holder = unresolvedRef.holder(pool);
        name = unresolvedRef.name(pool);
    }

    public final TypeDescriptor holder() {
        return holder;
    }

    public final Utf8Constant name() {
        return name;
    }

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();
}

/**
 * An abstract class that implements some of the functionality of a method or field entry in a constant pool that is in
 * a partially resolved state. That is, the references to the holder, name and signature of the field or method are
 * objects of type {@link ClassActor}, {@link Utf8Constant} and {@link Descriptor} respectively. That is, they are not
 * indices to other constant pool entries and as such, this class (and it subclasses) are useful for constructing pool
 * constants that don't rely on the existence of extra pool constants.
 */
abstract class UnresolvedRef<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends AbstractMemberRefConstant<PoolConstant_Type> {
    final ClassActor holder;
    final Utf8Constant name;
    final Descriptor descriptor;

    public UnresolvedRef(ClassActor holder, Utf8Constant name, Descriptor descriptor) {
        this.holder = holder;
        this.name = name;
        this.descriptor = descriptor;
    }

    public final TypeDescriptor holder(ConstantPool pool) {
        return holder();
    }

    public final Utf8Constant name(ConstantPool pool) {
        return name();
    }

    public final Descriptor descriptor(ConstantPool pool) {
        return descriptor;
    }

    public TypeDescriptor holder() {
        return holder.typeDescriptor;
    }

    public Utf8Constant name() {
        return name;
    }

    public final boolean isResolvableWithoutClassLoading(ConstantPool pool) {
        return true;
    }

    public final boolean isResolved() {
        return false;
    }

    public final SignatureDescriptor signature(ConstantPool pool) {
        return signature();
    }

    public final SignatureDescriptor signature() {
        try {
            return (SignatureDescriptor) descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(descriptor + " is not a valid method signature descriptor");
        }
    }

    public final TypeDescriptor type(ConstantPool pool) {
        return type();
    }

    public final TypeDescriptor type() {
        try {
            return (TypeDescriptor) descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(descriptor + " is not a valid field type descriptor");
        }
    }

    abstract boolean isFieldConstant();

    public final String valueString(ConstantPool pool) {
        if (isFieldConstant()) {
            return holder().toJavaString(true) + '.' + name() + ':' + type().toJavaString(false);
        }
        return holder().toJavaString(true) + '.' + name() + signature().toJavaString(false, false) + '.' + signature().resultDescriptor().toJavaString(false);
    }
}
