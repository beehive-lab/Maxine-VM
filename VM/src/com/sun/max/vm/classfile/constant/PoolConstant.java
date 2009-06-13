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

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
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
 *
 * @author Doug Simon
 * @author Bernd Mathiske
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
    }

}

//The rest of this file contains package-private abstract classes that provide most of the implementation
//for the pool constant interfaces.

/**
 * An abstract class that implements the most basic parts of the {@link PoolConstant} interface.
 */
abstract class AbstractPoolConstant<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> implements PoolConstant<PoolConstant_Type> {

    public abstract Tag tag();

    public abstract PoolConstantKey<PoolConstant_Type> key(ConstantPool pool);

    @Override
    public String toString() {
        return toString(null);
    }

    public final String toString(ConstantPool pool) {
        return Static.toString(this, pool);
    }
}

/**
 * An abstract class that implements some of the functionality of a method or field entry in a constant pool
 * that is in a completely unresolved state. That is, the references to the holder, name and signature of
 * the field or method are indices to other constant pool entries.
 */
abstract class UnresolvedRefIndices<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends AbstractPoolConstant<PoolConstant_Type> implements MemberRefConstant<PoolConstant_Type> {

    final int _classIndex;

    final int _nameAndTypeIndex;

    UnresolvedRefIndices(int classIndex, int nameAndTypeIndex, Tag[] tags) {
        _classIndex       = classIndex;
        _nameAndTypeIndex = nameAndTypeIndex;
        if (tags[classIndex] != CLASS) {
            throw ConstantPool.unexpectedEntry(classIndex, tags[classIndex], "defining class of field/method", CLASS);
        }
        if (tags[nameAndTypeIndex] != NAME_AND_TYPE) {
            throw ConstantPool.unexpectedEntry(nameAndTypeIndex, tags[nameAndTypeIndex], "field/method name and type", NAME_AND_TYPE);
        }
    }

    public final TypeDescriptor holder(ConstantPool pool) {
        final ClassConstant classRef = pool.classAt(_classIndex);
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

    public final boolean isResolvableWithoutClassLoading(ConstantPool pool) {
        final ClassConstant classConstant = pool.classAt(_classIndex);
        if (!classConstant.isResolvableWithoutClassLoading(pool)) {
            return false;
        }
        return true;
    }

    final NameAndTypeConstant nameAndType(ConstantPool pool) {
        return pool.nameAndTypeAt(_nameAndTypeIndex);
    }

    abstract boolean isFieldConstant();

    public final String valueString(ConstantPool pool) {
        if (pool == null) {
            return "classIndex=" + _classIndex + ",nameAndTypeIndex=" + _nameAndTypeIndex;
        }
        if (isFieldConstant()) {
            return holder(pool).toJavaString(true) + '.' + name(pool) + ':' + type(pool).toJavaString(false);
        }
        final SignatureDescriptor signature = signature(pool);
        return holder(pool).toJavaString(true) + "." + name(pool) + signature.toJavaString(false, false) + ":" + signature.resultDescriptor().toJavaString(false);
    }
}

abstract class RefKey {

    final TypeDescriptor _holder;
    final Utf8Constant _name;

    RefKey(ConstantPool pool, UnresolvedRefIndices unresolvedRef) {
        _holder = unresolvedRef.holder(pool);
        _name = unresolvedRef.name(pool);
    }

    public final TypeDescriptor holder() {
        return _holder;
    }

    public final Utf8Constant name() {
        return _name;
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
abstract class UnresolvedRef<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends AbstractPoolConstant<PoolConstant_Type> implements MemberRefConstant<PoolConstant_Type> {

    final ClassActor _holder;
    final Utf8Constant _name;
    final Descriptor _descriptor;

    public UnresolvedRef(ClassActor holder, Utf8Constant name, Descriptor descriptor) {
        _holder = holder;
        _name = name;
        _descriptor = descriptor;
    }

    public final TypeDescriptor holder(ConstantPool pool) {
        return holder();
    }

    public final Utf8Constant name(ConstantPool pool) {
        return name();
    }

    public final Descriptor descriptor(ConstantPool pool) {
        return _descriptor;
    }

    public TypeDescriptor holder() {
        return _holder.typeDescriptor();
    }

    public Utf8Constant name() {
        return _name;
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
            return (SignatureDescriptor) _descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(_descriptor + " is not a valid method signature descriptor");
        }
    }

    public final TypeDescriptor type(ConstantPool pool) {
        return type();
    }

    public final TypeDescriptor type() {
        try {
            return (TypeDescriptor) _descriptor;
        } catch (ClassCastException e) {
            throw classFormatError(_descriptor + " is not a valid field type descriptor");
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
