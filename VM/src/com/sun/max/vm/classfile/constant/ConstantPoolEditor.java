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

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;
import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;

import java.io.*;

/**
 * A mechanism for looking up and adding new entries to a constant pool. To ensure that
 * modifications of a constant pool that are performed in a thread safe manner,
 * {@linkplain ConstantPoolEditorClient an interface} is provided for use in conjunction with
 * {@link ConstantPool#edit(ConstantPoolEditorClient, boolean)}.
 *
 * @author Doug Simon
 */
public final class ConstantPoolEditor {

    private ConstantPool pool;
    private GrowableMapping<PoolConstantKey, Integer> constantsToIndices;
    private final Thread owner;
    private final boolean allowAppending;
    private int acquistionCount;

    /**
     * The amount by which the pool expands if extra capacity is needed when {@linkplain ConstantPoolEditor#append(PoolConstant) appending} new entries to the pool.
     */
    private static final int EXPANSION_AMOUNT = 10;

    /**
     * This must only be called from {@link ConstantPool#edit()}.
     */
    ConstantPoolEditor(ConstantPool pool, boolean allowAppending) {
        this.pool = pool;
        this.owner = Thread.currentThread();
        pool.editor = this;
        this.allowAppending = allowAppending;
        acquire();
    }

    /**
     * Gets the mapping from pool constants to indexes.
     * <p>
     * By constructing this data structure lazily, the cost of acquiring a ConstantPoolEditor is minimized - only upon
     * the first modification or search of the pool is a significant cost paid.
     */
    private GrowableMapping<PoolConstantKey, Integer> constantsToIndices() {
        if (constantsToIndices == null) {
            constantsToIndices = new HashEntryChainedHashMapping<PoolConstantKey, Integer>(pool.constants().length);

            // The pool is traversed in reverse so that the canonical index for a duplicated
            // constant is the lowest one. Most constant pools do not contain duplicate
            // entries but it is not illegal for them to do so. Not surprisingly, at least
            // one of the JCK tests contains such a constant pool.
            for (int index = pool.length - 1; index >= 1; --index) {
                final PoolConstant constant = pool.constants()[index];
                if (constant.tag() != INVALID) {
                    final PoolConstantKey key = constant.key(pool);
                    constantsToIndices.put(key, index);
                }
            }
        }
        return constantsToIndices;
    }

    /**
     * Creates and returns an editor on a copy of the pool being edited by this editor. This is typically used by a
     * process that will add extra entries to the pool for the purpose of writing a valid class file. Using a copy
     * prevents extra clutter from being added to the runtime version of a pool.
     */
    public ConstantPoolEditor copy() {
        final ConstantPool poolCopy = new ConstantPool(pool.classLoader(), pool.constants().clone(), pool.length);
        poolCopy.setHolder(poolCopy.holder());
        return new ConstantPoolEditor(poolCopy, true);
    }

    public ConstantPool pool() {
        return pool;
    }

    void acquire() {
        assert Thread.currentThread() == owner;
        ++acquistionCount;
        //if (_pool.holder() != null) System.err.printAddress(_owner + ": " + _acquistionCount + " acquired " + _pool);
    }

    public void release() {
        assert Thread.currentThread() == owner;
        assert pool != null && pool.editor == this;
        //if (_pool.holder() != null) System.err.printAddress(_owner + ": " + _acquistionCount + " releasing " + _pool);
        if (--acquistionCount <= 0) {
            synchronized (pool) {
                pool.notifyAll();
                pool.editor = null;
                pool = null;
            }
        }
    }

    public Thread owner() {
        return owner;
    }

    public int append(PoolConstant constant) {
        if (!allowAppending) {
            throw new IllegalStateException("Attempting to add an entry to " + pool());
        }
        if (pool.length == pool.constants().length) {
            final int newCapacity = pool.constants().length + ConstantPoolEditor.EXPANSION_AMOUNT;
            final PoolConstant[] newConstants = new PoolConstant[newCapacity];
            System.arraycopy(pool.constants(), 0, newConstants, 0, pool.length);
            pool.setConstants(newConstants);
        }

        final int index = pool.length;
        assert pool.constants()[index] == null;
        pool.setConstant(index, constant);
        ++pool.length;
        return index;
    }

    /**
     * Gets the index of a given constant in the constant pool. If {@code appendIfAbsent == true} and the constant
     * does not already exist in the pool, it's appended at the end.
     *
     * @return the index of {@code constant} in the pool or -1 if it's not in the pool and {@code appendIfAbsent == false}
     */
    public int indexOf(PoolConstant constant, boolean appendIfAbsent) {
        final PoolConstantKey key = constant instanceof PoolConstantKey ? (PoolConstantKey) constant : constant.key(pool);
        return find(key, constant, appendIfAbsent);
    }

    /**
     * Gets the index of a given constant in the constant pool. If the constant does not already exist in the pool,
     * it's appended at the end.
     */
    public int indexOf(PoolConstant constant) {
        return indexOf(constant, true);
    }

    private int find(PoolConstantKey key, PoolConstant constant, boolean appendIfAbsent) {
        final GrowableMapping<PoolConstantKey, Integer> map = constantsToIndices();
        Integer index = map.get(key);
        if (index == null) {
            if (!appendIfAbsent) {
                return -1;
            }
            index = append(constant);
            map.put(key, index);
        }
        return index.intValue();
    }

    /**
     * Writes the contents of the constant pool to a classfile stream.
     * <p>
     * This may cause extra entries to be added to the pool.
     */
    public void write(DataOutputStream stream) throws IOException {
        makeComplete();
        stream.writeShort(pool.numberOfConstants());
        for (int index = 1; index != pool.numberOfConstants(); ++index) {
            final PoolConstant constant = pool.at(index);
            final Tag tag = constant.tag();
            if (tag != INVALID) {
                stream.writeByte(tag.classfileTag());
                switch (tag) {
                    case UTF8: {
                        stream.writeUTF(pool.utf8At(index, null).toString());
                        break;
                    }
                    case CLASS: {
                        final String classDescriptor;
                        final String string = pool.classAt(index).typeDescriptor().toString();
                        if (string.charAt(0) == 'L') {
                            // Strip 'L' and ';' surrounding class name
                            classDescriptor = string.substring(1, string.length() - 1);
                        } else {
                            classDescriptor = string;
                        }
                        final int classIndex = indexOf(makeUtf8Constant(classDescriptor));
                        stream.writeShort(classIndex);
                        break;
                    }
                    case DOUBLE: {
                        stream.writeDouble(pool.doubleAt(index));
                        break;
                    }
                    case INTEGER: {
                        stream.writeInt(pool.intAt(index));
                        break;
                    }
                    case FLOAT: {
                        stream.writeFloat(pool.floatAt(index));
                        break;
                    }
                    case LONG: {
                        stream.writeLong(pool.longAt(index));
                        break;
                    }
                    case NAME_AND_TYPE: {
                        final NameAndTypeConstant nameAndType = pool.nameAndTypeAt(index);
                        stream.writeShort(indexOf(nameAndType.name()));
                        stream.writeShort(indexOf(makeUtf8Constant(nameAndType.descriptorString())));
                        break;
                    }
                    case INTERFACE_METHOD_REF:
                    case METHOD_REF:
                    case FIELD_REF: {
                        final MemberRefConstant field = pool.memberAt(index);
                        final int classIndex = indexOf(createClassConstant(field.holder(pool)));
                        final int nameAndTypeIndex = indexOf(createNameAndTypeConstant(field.name(pool), field.descriptor(pool)));
                        stream.writeShort(classIndex);
                        stream.writeShort(nameAndTypeIndex);
                        break;
                    }
                    case STRING: {
                        final String string = pool.stringAt(index);
                        stream.writeShort(indexOf(makeUtf8Constant(string)));
                        break;
                    }
                    default: {
                        ProgramError.unexpected("unknown tag: " + tag);
                    }
                }
            }
        }
    }

    /**
     * Creates any entries required for {@linkplain #write(DataOutputStream) writing out} a JVM specification compliant constant pool.
     */
    public void makeComplete() {
        for (int index = 1; index != pool.numberOfConstants(); ++index) {
            final PoolConstant constant = pool.at(index);
            final Tag tag = constant.tag();
            if (tag != INVALID) {
                switch (tag) {
                    case CLASS: {
                        final String classDescriptor;
                        final String string = pool.classAt(index).typeDescriptor().toString();
                        if (string.charAt(0) == 'L') {
                            // Strip 'L' and ';' surrounding class name
                            classDescriptor = string.substring(1, string.length() - 1);
                        } else {
                            classDescriptor = string;
                        }
                        indexOf(makeUtf8Constant(classDescriptor));
                        break;
                    }
                    case NAME_AND_TYPE: {
                        final NameAndTypeConstant nameAndType = pool.nameAndTypeAt(index);
                        indexOf(nameAndType.name());
                        indexOf(makeUtf8Constant(nameAndType.descriptorString()));
                        break;
                    }
                    case INTERFACE_METHOD_REF:
                    case METHOD_REF:
                    case FIELD_REF: {
                        final MemberRefConstant field = pool.memberAt(index);
                        indexOf(createClassConstant(field.holder(pool)));
                        indexOf(createNameAndTypeConstant(field.name(pool), field.descriptor(pool)));
                        break;
                    }
                    case STRING: {
                        final String string = pool.stringAt(index);
                        indexOf(makeUtf8Constant(string));
                        break;
                    }
                    default: {
                        // Entry does not cross-reference another entry
                    }
                }
            }
        }
    }
}
