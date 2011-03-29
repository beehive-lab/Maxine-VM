/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.lang.reflect.*;
import java.security.*;

import sun.misc.*;
import sun.reflect.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Method substitutions for {@link sun.misc.Unsafe}, which provides
 * low-level access to object memory.
 *
 */
@METHOD_SUBSTITUTIONS(Unsafe.class)
final class JDK_sun_misc_Unsafe {

    private JDK_sun_misc_Unsafe() {
    }

    /**
     * Register any native methods.
     */
    @SUBSTITUTE
    private static void registerNatives() {
    }

    /**
     * Reads an int field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getInt(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public int getInt(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readInt(0);
        }
        return Reference.fromJava(object).readInt(Offset.fromLong(offset));
    }

    /**
     * Writes an int into the object at the specified offset.
     * @see sun.misc.Unsafe#putInt(Object, long, int)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putInt(Object object, long offset, int value) {
        if (object == null) {
            Pointer.fromLong(offset).writeInt(0, value);
        }
        Reference.fromJava(object).writeInt(Offset.fromLong(offset), value);
    }

    /**
     * Reads an object (reference) field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getObject(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public Object getObject(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).getReference().toJava();
        }
        return Reference.fromJava(object).readReference(Offset.fromLong(offset)).toJava();
    }

    /**
     * Writes an object reference into the object at the specified offset.
     * @see sun.misc.Unsafe#putObject(Object, long, Object)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putObject(Object object, long offset, Object value) {
        if (object == null) {
            Pointer.fromLong(offset).writeReference(0, Reference.fromJava(value));
        }
        Reference.fromJava(object).writeReference(Offset.fromLong(offset), Reference.fromJava(value));
    }

    /**
     * Reads a boolean field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getBoolean(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public boolean getBoolean(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readBoolean(0);
        }
        return Reference.fromJava(object).readBoolean(Offset.fromLong(offset));
    }

    /**
     * Writes a boolean into the object at the specified offset.
     * @see sun.misc.Unsafe#putBoolean(Object, long, boolean)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putBoolean(Object object, long offset, boolean value) {
        if (object == null) {
            Pointer.fromLong(offset).writeBoolean(0, value);
        }
        Reference.fromJava(object).writeBoolean(Offset.fromLong(offset), value);
    }

    /**
     * Reads a byte field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getByte(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public byte getByte(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readByte(0);
        }
        return Reference.fromJava(object).readByte(Offset.fromLong(offset));
    }

    /**
     * Writes a byte into the object at the specified offset.
     * @see sun.misc.Unsafe#putByte(Object, long, byte)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putByte(Object object, long offset, byte value) {
        if (object == null) {
            Pointer.fromLong(offset).writeByte(0, value);
        }
        Reference.fromJava(object).writeByte(Offset.fromLong(offset), value);
    }

    /**
     * Reads a short field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getShort(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public short getShort(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readShort(0);
        }
        return Reference.fromJava(object).readShort(Offset.fromLong(offset));
    }

    /**
     * Writes a short into the object at the specified offset.
     * @see sun.misc.Unsafe#putShort(Object, long, short)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putShort(Object object, long offset, short value) {
        if (object == null) {
            Pointer.fromLong(offset).writeShort(0, value);
        }
        Reference.fromJava(object).writeShort(Offset.fromLong(offset), value);
    }

    /**
     * Reads a char field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getChar(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public char getChar(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readChar(0);
        }
        return Reference.fromJava(object).readChar(Offset.fromLong(offset));
    }

    /**
     * Writes a char into the object at the specified offset.
     * @see sun.misc.Unsafe#putChar(Object, long, char)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putChar(Object object, long offset, char value) {
        if (object == null) {
            Pointer.fromLong(offset).writeChar(0, value);
        }
        Reference.fromJava(object).writeChar(Offset.fromLong(offset), value);
    }

    /**
     * Reads a long field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getLong(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public long getLong(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readLong(0);
        }
        return Reference.fromJava(object).readLong(Offset.fromLong(offset));
    }

    /**
     * Writes a long into the object at the specified offset.
     * @see sun.misc.Unsafe#putLong(Object, long, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putLong(Object object, long offset, long value) {
        if (object == null) {
            Pointer.fromLong(offset).writeLong(0, value);
        }
        Reference.fromJava(object).writeLong(Offset.fromLong(offset), value);
    }

    /**
     * Reads a float field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getFloat(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public float getFloat(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readFloat(0);
        }
        return Reference.fromJava(object).readFloat(Offset.fromLong(offset));
    }

    /**
     * Writes a float into the object at the specified offset.
     * @see sun.misc.Unsafe#putFloat(Object, long, float)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putFloat(Object object, long offset, float value) {
        if (object == null) {
            Pointer.fromLong(offset).writeFloat(0, value);
        }
        Reference.fromJava(object).writeFloat(Offset.fromLong(offset), value);
    }

    /**
     * Reads a double field from the specified offset of the specified object.
     * @see sun.misc.Unsafe#getDouble(Object, long)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public double getDouble(Object object, long offset) {
        if (object == null) {
            return Pointer.fromLong(offset).readDouble(0);
        }
        return Reference.fromJava(object).readDouble(Offset.fromLong(offset));
    }

    /**
     * Writes a double into the object at the specified offset.
     * @see sun.misc.Unsafe#putDouble(Object, long, double)
     * @param object the object
     * @param offset the offset from the beginning of the object
     * @param value the value to write at the specified offset
     */
    @SUBSTITUTE
    public void putDouble(Object object, long offset, double value) {
        if (object == null) {
            Pointer.fromLong(offset).writeDouble(0, value);
        }
        Reference.fromJava(object).writeDouble(Offset.fromLong(offset), value);
    }

    /**
     * Reads a byte from the specified absolute address.
     * @see sun.misc.Unsafe#getByte(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public byte getByte(long address) {
        return Pointer.fromLong(address).readByte(0);
    }

    /**
     * Writes a byte to the specified absolute address.
     * @see sun.misc.Unsafe#putByte(long, byte)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putByte(long address, byte value) {
        Pointer.fromLong(address).writeByte(0, value);
    }

    /**
     * Reads a short from the specified absolute address.
     * @see sun.misc.Unsafe#getShort(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public short getShort(long address) {
        return Pointer.fromLong(address).readShort(0);
    }

    /**
     * Writes a short to the specified absolute address.
     * @see sun.misc.Unsafe#putShort(long, short)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putShort(long address, short value) {
        Pointer.fromLong(address).writeShort(0, value);
    }

    /**
     * Reads a char from the specified absolute address.
     * @see sun.misc.Unsafe#getChar(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public char getChar(long address) {
        return Pointer.fromLong(address).readChar(0);
    }

    /**
     * Writes a char to the specified absolute address.
     * @see sun.misc.Unsafe#putChar(long, char)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putChar(long address, char value) {
        Pointer.fromLong(address).writeChar(0, value);
    }

    /**
     * Reads an int from the specified absolute address.
     * @see sun.misc.Unsafe#getInt(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public int getInt(long address) {
        return Pointer.fromLong(address).readInt(0);
    }

    /**
     * Writes an int to the specified absolute address.
     * @see sun.misc.Unsafe#putInt(long, int)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putInt(long address, int value) {
        Pointer.fromLong(address).writeInt(0, value);
    }

    /**
     * Reads a long from the specified absolute address.
     * @see sun.misc.Unsafe#getLong(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public long getLong(long address) {
        return Pointer.fromLong(address).readLong(0);
    }

    /**
     * Writes a long to the specified absolute address.
     * @see sun.misc.Unsafe#putLong(long, long)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putLong(long address, long value) {
        Pointer.fromLong(address).writeLong(0, value);
    }

    /**
     * Reads a float from the specified absolute address.
     * @see sun.misc.Unsafe#getFloat(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public float getFloat(long address) {
        return Pointer.fromLong(address).readFloat(0);
    }

    /**
     * Writes a float to the specified absolute address.
     * @see sun.misc.Unsafe#putFloat(long, float)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putFloat(long address, float value) {
        Pointer.fromLong(address).writeFloat(0, value);
    }

    /**
     * Reads a double from the specified absolute address.
     * @see sun.misc.Unsafe#getDouble(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public double getDouble(long address) {
        return Pointer.fromLong(address).readDouble(0);
    }

    /**
     * Writes a double to the specified absolute address.
     * @see sun.misc.Unsafe#putDouble(long, double)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putDouble(long address, double value) {
        Pointer.fromLong(address).writeDouble(0, value);
    }

    /**
     * Reads an address from the specified absolute address.
     * @see sun.misc.Unsafe#getAddress(long)
     * @param address the address from which to read the value
     * @return the value at the specified address
     */
    @SUBSTITUTE
    public long getAddress(long address) {
        return Pointer.fromLong(address).readWord(0).asAddress().toLong();
    }

    /**
     * Writes an address to the specified absolute address.
     * @see sun.misc.Unsafe#putAddress(long, long)
     * @param address the address to which to write the value
     * @param value the value to write
     */
    @SUBSTITUTE
    public void putAddress(long address, long value) {
        Pointer.fromLong(address).writeWord(0, Address.fromLong(value));
    }

    /**
     * Allocates raw, ummovable memory.
     * @see Unsafe#allocateMemory(long)
     * @param bytes the number of bytes to allocate
     * @return the address of a chunk of memory of the specified size
     */
    @SUBSTITUTE
    public long allocateMemory(long bytes) {
        if (bytes < 0L || bytes > Word.widthValue().max) {
            throw new IllegalArgumentException();
        }
        Pointer address = Memory.allocate(Size.fromLong(bytes));
        if (address.isZero()) {
            throw new OutOfMemoryError();
        }
        return address.toLong();
    }

    /**
     * Attempts to resize an allocated chunk of memory.
     * @see Unsafe#reallocateMemory(long, long)
     * @param address the original address of the block of memory
     * @param bytes the total number of bytes to allocate for the new chunk
     * @return the address of a chunk of memory of the new size
     */
    @SUBSTITUTE
    public long reallocateMemory(long address, long bytes) {
        if (bytes == 0) {
            return 0L;
        }
        if (bytes < 0L || bytes > Word.widthValue().max) {
            throw new IllegalArgumentException();
        }
        return Memory.reallocate(Pointer.fromLong(address), Size.fromLong(bytes)).toLong();
    }

    /**
     * Initializes a range of memory to a known value.
     * @see Unsafe#setMemory(long, long, byte)
     * @param address the starting address to initialize
     * @param bytes the number of bytes to write
     * @param value the value to fill the memory with
     */
    @SUBSTITUTE
    public void setMemory(long address, long bytes, byte value) {
        Memory.setBytes(Pointer.fromLong(address), Size.fromLong(bytes), value);
    }

    /**
     * Copies a range memory to another range of memory.
     * @see Unsafe#copyMemory(long, long, long)
     * @param srcAddress the source address
     * @param destAddress the destination address
     * @param bytes the number of bytes to copy
     */
    @SUBSTITUTE
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        Memory.copyBytes(Pointer.fromLong(srcAddress), Pointer.fromLong(destAddress), Size.fromLong(bytes));
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The transfers are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective addresses and
     * length are all even modulo 8, the transfer takes place in 'long' units.
     * If the effective addresses and length are (resp.) even modulo 4 or 2,
     * the transfer takes place in units of 'int' or 'short'.
     *
     * @since 1.7
     */
    @SUBSTITUTE(optional = true)
    public void copyMemory(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        Pointer src;
        if (srcBase == null) {
            src = Pointer.fromLong(srcOffset);
        } else {
            src = Reference.fromJava(srcBase).toOrigin().plus(srcOffset);
        }
        Pointer dest;
        if (destBase == null) {
            dest = Pointer.fromLong(destOffset);
        } else {
            dest = Reference.fromJava(destBase).toOrigin().plus(destOffset);
        }
        Memory.copyBytes(src, dest, Size.fromLong(bytes));
    }

    /**
     * Free a chunk of previously allocated memory.
     * @see Unsafe#freeMemory(long)
     * @param address the address of the beginning of a chunk of memory to delete
     */
    @SUBSTITUTE
    public void freeMemory(long address) {
        if (address != 0L) {
            Memory.deallocate(Pointer.fromLong(address));
        }
    }

    /**
     * Gets the offset of a declared static field from the start of its class.
     * @see Unsafe#staticFieldOffset(Field)
     * @param field the field for which to get offset
     * @return the offset for the field from the beginning of its class's static fields
     */
    @SUBSTITUTE
    public long staticFieldOffset(Field field) {
        return FieldActor.fromJava(field).offset();
    }

    /**
     * Gets the offset of a declared field from the start of an object of its type.
     * @see Unsafe#objectFieldOffset(Field)
     * @param field the field for which to get offset
     * @return the offset for the field from the beginning of an object of the appropriate type
     */
    @SUBSTITUTE
    public long objectFieldOffset(Field field) {
        return FieldActor.fromJava(field).offset();
    }

    /**
     * Gets the base address for static fields.
     * @see Unsafe#staticFieldBase(Field)
     * @param field the field for which to get the base
     * @return an address representing the base for a static field's offset
     */
    @SUBSTITUTE
    public Object staticFieldBase(Field field) {
        return FieldActor.fromJava(field).holder().staticTuple();
    }

    /**
     * Forces a class to be initialized.
     * @see Unsafe#ensureClassInitialized(Class)
     * @param c the class to initialize
     */
    @SUBSTITUTE
    public void ensureClassInitialized(Class c) {
        Snippets.makeClassInitialized(ClassActor.fromJava(c));
    }

    /**
     * Gets the base offset (i.e. offset of element 0) for arrays of the specified class.
     * @see Unsafe#arrayBaseOffset(Class)
     * @param arrayClass the class for which to get the base offset
     * @return an integer representing the base offset
     */
    @SUBSTITUTE
    public int arrayBaseOffset(Class arrayClass) {
        final ArrayClassActor arrayClassActor = (ArrayClassActor) ClassActor.fromJava(arrayClass);
        final ArrayLayout arrayLayout = (ArrayLayout) arrayClassActor.dynamicHub().specificLayout;
        return arrayLayout.getElementOffsetFromOrigin(0).toInt();
    }

    /**
     * Gets the scale (element size) for elements of the specified array class.
     * @see Unsafe#arrayIndexScale(Class)
     * @param arrayClass the array class for which to get the base offset
     * @return an integer representing the element size
     */
    @SUBSTITUTE
    public int arrayIndexScale(Class arrayClass) {
        final ArrayClassActor arrayClassActor = (ArrayClassActor) ClassActor.fromJava(arrayClass);
        return arrayClassActor.componentClassActor().kind.width.numberOfBytes;
    }

    /**
     * Return the size of an address in bytes.
     * @see Unsafe#addressSize()
     * @return the size of an address
     */
    @SUBSTITUTE
    public int addressSize() {
        return Word.size();
    }

    /**
     * Return the size of a page on this platform.
     * @see Unsafe#pageSize()
     * @return the size in bytes of a page
     */
    @SUBSTITUTE
    public int pageSize() {
        return platform().pageSize;
    }

    /**
     * Create a class from the specified parameters.
     * @see Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)
     * @param name the name of the new class
     * @param bytes a byte array containing the class file
     * @param offset the offset in the byte array for the start of the classfile
     * @param length the length of the classfile
     * @param loader the classloader in which to create the class
     * @param protectionDomain the protection domain for the new class
     * @return a new class from the specified parameters
     */
    @SUBSTITUTE
    public Class defineClass(String name, byte[] bytes, int offset, int length, ClassLoader loader, ProtectionDomain protectionDomain) {
        return ClassfileReader.defineClassActor(name, loader, bytes, offset, length, protectionDomain, null, false).toJava();
    }

    /**
     * Create a class from the specified parameters.
     * @see Unsafe#defineClass(String, byte[], int, int, ClassLoader, ProtectionDomain)
     * @param name the name of the new class
     * @param bytes a byte array containing the class file
     * @param offset the offset in the byte array for the start of the classfile
     * @param length the length of the classfile
     * @param loader the classloader in which to create the class
     * @return a new class from the specified parameters
     */
    @SUBSTITUTE
    public Class defineClass(String name, byte[] bytes, int offset, int length) {
        final Class currentClass = Reflection.getCallerClass(2);
        return defineClass(name, bytes, offset, length, currentClass.getClassLoader(), currentClass.getProtectionDomain());
    }

    /**
     * Creates an instance of the specified class, without the typical safety checks.
     * @see Unsafe#allocateInstance(Class)
     * @param javaClass the class to create
     * @return a new instance of the specified class
     */
    @SUBSTITUTE
    public Object allocateInstance(Class javaClass) {
        final ClassActor classActor = ClassActor.fromJava(javaClass);
        Snippets.makeClassInitialized(classActor);
        if (classActor.isArrayClass()) {
            return Heap.createArray(classActor.dynamicHub(), 0);
        }
        if (classActor.isTupleClass()) {
            return Heap.createTuple(classActor.dynamicHub());
        }
        return null;
    }

    /**
     * Attempt to acquire the monitor for an object.
     * @see Unsafe#monitorEnter(Object)
     * @param object the object for which to acquire the monitor
     */
    @SUBSTITUTE
    public void monitorEnter(Object object) {
        vmConfig().monitorScheme().monitorEnter(object);
    }

    /**
     * Release the monitor of an object.
     * @see Unsafe#monitorExit(Object)
     * @param object the object for which to release the monitor
     */
    @SUBSTITUTE
    public void monitorExit(Object object) {
        vmConfig().monitorScheme().monitorExit(object);
    }

    /**
     * Throw an exception.
     * @see Unsafe#throwException(Throwable)
     * @param throwable the exception to throw
     * @throws Throwable always
     */
    @SUBSTITUTE
    public void throwException(Throwable throwable) throws Throwable {
        throw throwable;
    }

    /**
     * Compare and swap an object reference atomically.
     * @param object the object in which the reference is stored
     * @param offset the offset from the beginning of the object
     * @param expected the object reference to compare against
     * @param value the new value to write
     * @return true if the old value read is equal to the expected object; false otherwise
     */
    @SUBSTITUTE
    public boolean compareAndSwapObject(Object object, long offset, Object expected, Object value) {
        return Reference.fromJava(object).compareAndSwapReference(Offset.fromLong(offset), Reference.fromJava(expected), Reference.fromJava(value)) == expected;
    }

    /**
     * Compare and swap an integer value atomically.
     * @param object the object in which the int is stored
     * @param offset the offset from the beginning of the object
     * @param expected the int value to compare against
     * @param value the new value to write
     * @return true if the old value read is equal to the expected value; false otherwise
     */
    @SUBSTITUTE
    public boolean compareAndSwapInt(Object object, long offset, int expected, int value) {
        return Reference.fromJava(object).compareAndSwapInt(Offset.fromLong(offset), expected, value) == expected;
    }

    /**
     * Compare and swap a long value atomically.
     * @param object the object in which the long is stored
     * @param offset the offset from the beginning of the object
     * @param expected the long value to compare against
     * @param value the new value to write
     * @return true if the old value read is equal to the expected value; false otherwise
     */
    @SUBSTITUTE
    public boolean compareAndSwapLong(Object object, long offset, long expected, long value) {
        return Reference.fromJava(object).compareAndSwapWord(Offset.fromLong(offset), Address.fromLong(expected), Address.fromLong(value)) == Address.fromLong(expected);
    }

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INLINE
    private static void preVolatileRead() {
        // no-op
    }

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_LOAD | LOAD_STORE) << 8))
    private static native void postVolatileRead();

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_STORE | STORE_STORE) << 8))
    private static native void preVolatileWrite();

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((STORE_LOAD | STORE_STORE) << 8))
    private static native void postVolatileWrite();

    /**
     * Reads a volatile object field from an object at the specified offset.
     * @see Unsafe#getObjectVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public Object getObjectVolatile(Object object, long offset) {
        preVolatileRead();
        Object value = getObject(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile object field into the object at the specified offset.
     * @see Unsafe#putObjectVolatile(Object, long, Object)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putObjectVolatile(Object object, long offset, Object value) {
        preVolatileWrite();
        putObject(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile int field from an object at the specified offset.
     * @see Unsafe#getIntVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public int getIntVolatile(Object object, long offset) {
        preVolatileRead();
        int value = getInt(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile int field into the object at the specified offset.
     * @see Unsafe#putIntVolatile(Object, long, int)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putIntVolatile(Object object, long offset, int value) {
        preVolatileWrite();
        putInt(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile boolean field from an object at the specified offset.
     * @see Unsafe#getBooleanVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public boolean getBooleanVolatile(Object object, long offset) {
        preVolatileRead();
        boolean value = getBoolean(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile boolean field into the object at the specified offset.
     * @see Unsafe#putBooleanVolatile(Object, long, boolean)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putBooleanVolatile(Object object, long offset, boolean value) {
        preVolatileWrite();
        putBoolean(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile byte field from an object at the specified offset.
     * @see Unsafe#getByteVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public byte getByteVolatile(Object object, long offset) {
        preVolatileRead();
        byte value = getByte(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile byte field into the object at the specified offset.
     * @see Unsafe#putByteVolatile(Object, long, byte)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putByteVolatile(Object object, long offset, byte value) {
        preVolatileWrite();
        putByte(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile short field from an object at the specified offset.
     * @see Unsafe#getShortVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public short getShortVolatile(Object object, long offset) {
        preVolatileRead();
        short value = getShort(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile short field into the object at the specified offset.
     * @see Unsafe#putShortVolatile(Object, long, short)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putShortVolatile(Object object, long offset, short value) {
        preVolatileWrite();
        putShort(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile char field from an object at the specified offset.
     * @see Unsafe#getObjectVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public char getCharVolatile(Object object, long offset) {
        preVolatileRead();
        char value = getChar(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile char field into the object at the specified offset.
     * @see Unsafe#putCharVolatile(Object, long, char)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putCharVolatile(Object object, long offset, char value) {
        preVolatileWrite();
        putChar(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile long field from an object at the specified offset.
     * @see Unsafe#getLongVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public long getLongVolatile(Object object, long offset) {
        preVolatileRead();
        long value = getLong(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile long field into the object at the specified offset.
     * @see Unsafe#putLongVolatile(Object, long, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putLongVolatile(Object object, long offset, long value) {
        preVolatileWrite();
        putLong(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile float field from an object at the specified offset.
     * @see Unsafe#getFloatVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public float getFloatVolatile(Object object, long offset) {
        preVolatileRead();
        float value = getFloat(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile float field into the object at the specified offset.
     * @see Unsafe#putFloatVolatile(Object, long, float)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putFloatVolatile(Object object, long offset, float value) {
        preVolatileWrite();
        putFloat(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Reads a volatile double field from an object at the specified offset.
     * @see Unsafe#getDoubleVolatile(Object, long)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @return the value at the specified offset
     */
    @SUBSTITUTE
    public double getDoubleVolatile(Object object, long offset) {
        preVolatileRead();
        double value = getDouble(object, offset);
        postVolatileRead();
        return value;
    }

    /**
     * Writes a volatile double field into the object at the specified offset.
     * @see Unsafe#putDoubleVolatile(Object, long, double)
     * @param object the object containing the field
     * @param offset the offset from the beginning of the object
     * @param value the new value to write into the field
     */
    @SUBSTITUTE
    public void putDoubleVolatile(Object object, long offset, double value) {
        preVolatileWrite();
        putDouble(object, offset, value);
        postVolatileWrite();
    }

    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     */
    @SUBSTITUTE
    public void putOrderedObject(Object o, long offset, Object x) {
        // no optimization yet
        putObjectVolatile(o, offset, x);
    }

    /**
     * Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}.
     */
    @SUBSTITUTE
    public void putOrderedInt(Object o, long offset, int x) {
        // no optimization yet
        putIntVolatile(o, offset, x);
    }

    /**
     * Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)}.
     */
    @SUBSTITUTE
    public void putOrderedLong(Object o, long offset, long x) {
        // no optimization yet
        putLongVolatile(o, offset, x);
    }

    /**
     * @see Unsafe#unpark(Object)
     * @param thread
     */
    @SUBSTITUTE
    public void unpark(Object javaThread) {
        final VmThread thread = VmThread.fromJava((Thread) javaThread);
        if (thread != null) {
            thread.unpark();
        }
    }

    /**
     * @see Unsafe#park(boolean, long)
     * @param isAbsolute
     * @param time
     */
    @SUBSTITUTE
    public void park(boolean isAbsolute, long time) {
        final VmThread thread = VmThread.current();
        try {
            if (!isAbsolute) {
                thread.park(time);
            } else {
                thread.park();
            }
        } catch (InterruptedException e) {
            // do nothing.
        }
    }
}
