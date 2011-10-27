/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.intrinsics;

import com.sun.cri.bytecode.*;
import com.sun.max.vm.runtime.*;

/**
 * Definition of ID strings for Maxine-specific intrinsics.
 */
public class MaxineIntrinsicIDs {
    /**
     * Prefix of ID strings defined in this class to make them unique.
     */
    private static final String p = "com.oracle.max.vm.intrinsics:";

    /**
     * Unsafe cast of a value to a different Java type, without any code emitted for the cast.
     * <p>
     * The method definition must have one of the following forms:
     * <pre>
     * static U m(T value)
     * U T.m()
     * where the value is cast from T to U. In the case of a non-static method definition, the this-pointer is cast.
     *     T and U must have the same register kind, i.e., it cannot be used to cast between integer and floating point
     *     types, or between 32-bit and 64-bit values.
     * </pre>
     */
    public static final String UNSAFE_CAST = p + "UNSAFE_CAST";

    /**
     * Allocates a requested block of memory within the current activation frame.
     * The allocated memory is reclaimed when the method returns.
     *
     * The allocation is for the lifetime of the method execution. That is, the compiler
     * reserves the space in the compiled size of the frame. As such, a failure
     * to allocate the requested space will result in a {@link StackOverflowError}
     * when the method's prologue is executed.
     *
     * The value on the top of the stack is the size in bytes to allocate.
     * The result is the address of the allocated block. <b>N.B.</b> The contents of the block are uninitialized.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static Pointer m(@INTRINSIC.Constant int size);
     * size: The number of bytes to allocate on the stack.
     *     This parameter must be a compile-time constant.
     * </pre>
     */
    public static final String ALLOCA = p + "ALLOCA";

    /**
     * Create a handle for a given value on the native stack frame.
     * <p>
     * Forces the compiler to allocate a native frame slot and initializes it with {@code value}
     * If the {@code value} is 0 or {@code null}, then the result is 0 otherwise the result is the
     * address of the native frame slot.
     * <p>
     * The native frame slot will be live for the rest of the method. The type of {@code value} determines
     * if it is a GC root. If {@code value} is an object, any subsequent value written to the slot
     * (via {@code address}) must be an object. If {@code value} is not an object, any subsequent value
     * written to the slot must not be an object.
     * <b>The compiler is not required to enforce this type safety.</b>
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static Pointer m(T value);
     * </pre>
     */
    public static final String STACKHANDLE = p + "STACKHANDLE";

    /**
     * If the CPU supports it, then this intrinsic issues an instruction that improves the performance of spin loops by
     * providing a hint to the processor that the current thread is in a spin loop. The processor may use this to
     * optimize power consumption while in the spin loop.
     *
     * If the CPU does not support such an instruction, then nothing is emitted for this intrinsic.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static void m()
     * </pre>
     */
    public static final String PAUSE = p + "PAUSE";

    /**
     * Inserts machine code to generate a breakpoint trap.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static void m()
     * </pre>
     */
    public static final String BREAKPOINT_TRAP = p + "BREAKPOINT_TRAP";

    /**
     * Returns the index of the least significant bit set in a given value.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static int m(Word value)
     * value: the value to scan for the least significant bit
     * returns: the index of the least significant bit within value, or -1 if value == 0
     * </pre>
     */
    public static final String LSB = p + "LSB";

    /**
     * Returns the index of the most significant bit set in a given value.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static int m(Word value)
     * value: the value to scan for the least significant bit
     * returns: the index of the most significant bit within value, or -1 if value == 0
     * </pre>
     */
    public static final String MSB = p + "MSB";

    /**
     * Reads the value of a register playing a runtime-defined role.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static Word m(@INTRINSIC.Constant int registerId);
     * registerId: The {@link VMRegister register id} of the register to read.
     *     This parameter must be a compile-time constant.
     * </pre>
     */
    public static final String READREG = p + "READREG";

    /**
     * Writes the value of a register playing a runtime-defined role.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static void m(@INTRINSIC.Constant int registerId, Word value);
     * registerId: The {@link VMRegister register id} of the register to write.
     *     This parameter must be a compile-time constant.
     * value: The value to write into the register.
     * </pre>
     */
    public static final String WRITEREG = p + "WRITEREG";

    /**
     * Reads/tests the value of a bit specified by the {@link VMRegister#LATCH} register, a byte offset,
     * and a bit number (LSB=0), that will be then used to branch on the value. I.e. this is not a general
     * purpose bit reading mechanism.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static boolean m(@INTRINSIC.Constant int offset, @INTRINSIC.Constant int bit);
     * offset: The offset of the address to read.
     * bit: The bit of the memory cell to read.
     *     Both parameters must be compile-time constants.
     *     The call of the intrinsic must be followed by an {@link Bytecodes#IFEQ} or {@link Bytecodes#IFNE} bytecode.
     * </pre>
     */
    public static final String IFLATCHBITREAD = p + "IFLATCHBITREAD";

    /**
     * Read a value from memory.
     * <p>
     * The method definition must have one of the following forms:
     * <pre>
     * T Pointer.m(int offset)
     * T Pointer.readByte(Offset offset)
     * T Pointer.getByte(int displacement, int index)
     * where T is any Java type (primitive type, Object, or Word)
     *
     * this: The base of the address to read.
     * offset: The offset of the address to read (when the 'base + offset' addressing mode is used).
     * displacement, index: The displacement and index of the address to read (when the 'base + displacement + index * n
     *     addressing mode is used; n is the size of type T).
     * </pre>
     */
    public static final String PREAD = p + "PREAD";

    /**
     * Read a value to memory.
     * <p>
     * The method definition must have one of the following forms:
     * <pre>
     * void Pointer.m(int offset, T value)
     * void Pointer.readByte(Offset offset, T value)
     * void Pointer.getByte(int displacement, int index, T value)
     * where T is any Java type (primitive type, Object, or Word)
     *
     * this: The base of the address to read.
     * offset: The offset of the address to read (when the 'base + offset' addressing mode is used).
     * displacement, index: The displacement and index of the address to read (when the 'base + displacement + index * n
     *     addressing mode is used; n is the size of type T).
     * value: The value to write.
     * </pre>
     */
    public static final String PWRITE = p + "PWRITE";

    /**
     * Atomic update of a value in memory.
     *
     * Compares {@code expectedValue} value with the actual value in a memory location (given by {@code pointer + offset}).
     * Iff they are same, {@code newValue} is stored into the memory location and the {@code expectedValue} is returned.
     * Otherwise, the actual value is returned.
     * <p>
     * The method definition must have one of the following forms:
     * <pre>
     * T Pointer.compareAndSwapInt(int offset, T expectedValue, T newValue)
     * T Pointer.compareAndSwapInt(Offset offset, T expectedValue, T newValue)
     * where T is one of {int, long, Object}
     *
     * this: The base of the address to read.
     * offset: The offset of the address to read.
     * expectedValue: if this value is currently in the memory location, perform the swap
     * newValue: the new value to store into the memory location
     * returns: either expectedValue or the actual value
     * </pre>
     */
    public static final String PCMPSWP = p + "PCMPSWP";

    /**
     * Record debug info at the current code location
     * and emit the instruction(s) that enable a thread
     * to be safely stopped for a VM operation (e.g. a GC).
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static void m();
     * </pre>
     */
    public static final String SAFEPOINT_POLL = p + "INFOPOINT:SAFEPOINT_POLL";

    /**
     * Record debug info at the current code location
     * and push its address to the stack. This is useful (for example)
     * when initiating a stack walk from the current execution position.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static long m();
     * </pre>
     */
    public static final String HERE = p + "INFOPOINT:HERE";

    /**
     * Record debug info at the current code location.
     * No instructions are emitted.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static void m();
     * </pre>
     */
    public static final String INFO = p + "INFOPOINT:INFO";

    /**
     * Record debug info at the current code location and deoptimize if it is executed.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static void m();
     * </pre>
     */
    public static final String UNCOMMON_TRAP = p + "INFOPOINT:UNCOMMON_TRAP";

    /**
     * Emulate one of the raw comparison bytecodes for long, float, or double. The returned value
     * adheres to the Java bytecode specification of the given opcode.
     * <p>
     * The method definition must have the following form:
     * <pre>
     * static int m(@INTRINSIC.Constant int opcode, T l, T r);
     * where T is one of {long, float, double}
     *
     * opcode: One of the following opcodes: {@link Bytecodes#LCMP}, {@link Bytecodes#FCMPL}, {@link Bytecodes#FCMPG}, {@link Bytecodes#DCMPL}, {@link Bytecodes#DCMPG}
     *     This parameter must be a compile-time constant.
     * l, r: The values to compare. The type T must match the type of the opcode.
     * </pre>
     */
    public static final String CMP_BYTECODE = p + "CMP_BYTECODE";
}

