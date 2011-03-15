/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * This class offers stack frame operations for use in bytecode templates, including
 * pushing and popping values from the operand stack, accessing local variables, and
 * making Java calls.
 *
 * This class assumes that the ABI stack pointer is used as the java stack pointer,
 * and when these methods are inlined into bytecode templates, they result in code that
 * indexes directly off of the ABI stack pointer, which points into the java operand
 * stack. Similarly, this class assumes the ABI frame pointer is used to access the
 * local variables.
 *
 * @author Laurent Daynes
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Michael Bebenita
 */
public final class T1XFrameOps {
    private T1XFrameOps() {
    }

    private static final int WORDS_PER_SLOT = JVMS_SLOT_SIZE / Word.size();
    private static final int BIAS = JVMS_STACK_BIAS;

    private static final int HALFWORD_OFFSET_IN_WORD = JVMSFrameLayout.offsetWithinWord(Kind.INT);

    @INLINE
    public static void addSlots(int numberOfSlots) {
        VMRegister.adjustAbiStackPointer(-(numberOfSlots * JVMS_SLOT_SIZE));
    }

    @INLINE
    public static void removeSlots(int numberOfSlots) {
        VMRegister.adjustAbiStackPointer(numberOfSlots * JVMS_SLOT_SIZE);
    }

    @INLINE
    public static Word peekWord(int index) {
        return VMRegister.getAbiStackPointer().getWord(BIAS, index * WORDS_PER_SLOT);
    }

    @INLINE
    public static int peekInt(int index) {
        return VMRegister.getAbiStackPointer().readInt(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE + HALFWORD_OFFSET_IN_WORD);
    }

    @INLINE
    public static float peekFloat(int index) {
        return VMRegister.getAbiStackPointer().readFloat(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE + HALFWORD_OFFSET_IN_WORD);
    }

    @INLINE
    public static long peekLong(int index) {
        return VMRegister.getAbiStackPointer().readLong(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE);
    }

    @INLINE
    public static double peekDouble(int index) {
        return VMRegister.getAbiStackPointer().readDouble(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE);
    }

    @INLINE
    public static Object peekObject(int index) {
        return VMRegister.getAbiStackPointer().getReference(BIAS, index * WORDS_PER_SLOT).toJava();
    }

    @INLINE
    public static Reference peekReference(int index) {
        return VMRegister.getAbiStackPointer().getReference(BIAS, index * WORDS_PER_SLOT);
    }

    @INLINE
    public static void pokeWord(int index, Word value) {
        VMRegister.getAbiStackPointer().setWord(BIAS, index * WORDS_PER_SLOT, value);
    }

    @INLINE
    public static void pokeInt(int index, int value) {
        VMRegister.getAbiStackPointer().writeInt(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE + HALFWORD_OFFSET_IN_WORD, value);
    }

    @INLINE
    public static void pokeFloat(int index, float value) {
        VMRegister.getAbiStackPointer().writeFloat(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE + HALFWORD_OFFSET_IN_WORD, value);
    }

    @INLINE
    public static void pokeLong(int index, long value) {
        VMRegister.getAbiStackPointer().writeLong(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE, value);
    }

    @INLINE
    public static void pokeDouble(int index, double value) {
        VMRegister.getAbiStackPointer().writeDouble(BIAS + index * JVMSFrameLayout.JVMS_SLOT_SIZE, value);
    }

    @INLINE
    public static void pokeReference(int index, Object value) {
        VMRegister.getAbiStackPointer().setReference(BIAS, index * WORDS_PER_SLOT, Reference.fromJava(value));
    }

    @INLINE
    public static float popFloat() {
        final float value = peekFloat(0);
        removeSlots(1);
        return value;
    }

    @INLINE
    public static int popInt() {
        final int value = peekInt(0);
        removeSlots(1);
        return value;
    }

    @INLINE
    public static long popLong() {
        final long value = peekLong(0);
        removeSlots(2);
        return value;
    }

    @INLINE
    public static double popDouble() {
        final double value = peekDouble(0);
        removeSlots(2);
        return value;
    }

    @INLINE
    public static void pushWord(final Word value) {
        addSlots(1);
        pokeWord(0, value);
    }

    @INLINE
    public static void pushFloat(final float value) {
        addSlots(1);
        pokeFloat(0, value);
    }

    @INLINE
    public static void pushInt(final int value) {
        addSlots(1);
        pokeInt(0, value);
    }

    @INLINE
    public static void pushObject(final Object value) {
        addSlots(1);
        pokeReference(0, value);
    }

    @INLINE
    public static void pushLong(final long value) {
        addSlots(2);
        pokeLong(0, value);
    }

    @INLINE
    public static void pushDouble(final double value) {
        addSlots(2);
        pokeDouble(0, value);
    }

    @INLINE
    public static void setLocalObject(int slotOffset, Object value) {
        VMRegister.getAbiFramePointer().writeReference(slotOffset, Reference.fromJava(value));
    }

    @INLINE
    public static void setLocalWord(int slotOffset, Word value) {
        VMRegister.getAbiFramePointer().writeWord(slotOffset, value);
    }

    @INLINE
    public static void setLocalInt(int slotOffset, int value) {
        VMRegister.getAbiFramePointer().writeInt(slotOffset, value);
    }

    @INLINE
    public static void setLocalFloat(int slotOffset, float value) {
        VMRegister.getAbiFramePointer().writeFloat(slotOffset, value);
    }

    @INLINE
    public static void setLocalLong(int slotOffset, long value) {
        VMRegister.getAbiFramePointer().writeLong(slotOffset, value);
    }

    @INLINE
    public static void setLocalDouble(int slotOffset, double value) {
        VMRegister.getAbiFramePointer().writeDouble(slotOffset, value);
    }

    @INLINE
    public static Object getLocalObject(int slotOffset) {
        return VMRegister.getAbiFramePointer().readReference(slotOffset).toJava();
    }

    @INLINE
    public static Word getLocalWord(int slotOffset) {
        return VMRegister.getAbiFramePointer().readWord(slotOffset);
    }

    @INLINE
    public static int getLocalInt(int slotOffset) {
        return VMRegister.getAbiFramePointer().readInt(slotOffset);
    }

    @INLINE
    public static float getLocalFloat(int slotOffset) {
        return VMRegister.getAbiFramePointer().readFloat(slotOffset);
    }

    @INLINE
    public static long getLocalLong(int slotOffset) {
        return VMRegister.getAbiFramePointer().readLong(slotOffset);
    }

    @INLINE
    public static double getLocalDouble(int slotOffset) {
        return VMRegister.getAbiFramePointer().readDouble(slotOffset);
    }

    @INLINE
    public static void directCallVoid() {
        SpecialBuiltin.call();
    }

    @INLINE
    public static void directCallFloat() {
        final float result = SpecialBuiltin.callFloat();
        pushFloat(result);
    }

    @INLINE
    public static void directCallLong() {
        final long result = SpecialBuiltin.callLong();
        pushLong(result);
    }

    @INLINE
    public static void directCallDouble() {
        final double result = SpecialBuiltin.callDouble();
        pushDouble(result);
    }

    @INLINE
    public static void directCallWord() {
        final Word result = SpecialBuiltin.callWord();
        pushWord(result);
    }

    @INLINE
    public static void indirectCallVoid(Address address, CallEntryPoint callEntryPoint) {
        SpecialBuiltin.call(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()));
    }

    @INLINE
    public static void indirectCallFloat(Address address, CallEntryPoint callEntryPoint) {
        final float result = SpecialBuiltin.callFloat(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()));
        pushFloat(result);
    }

    @INLINE
    public static void indirectCallLong(Address address, CallEntryPoint callEntryPoint) {
        final long result = SpecialBuiltin.callLong(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()));
        pushLong(result);
    }

    @INLINE
    public static void indirectCallDouble(Address address, CallEntryPoint callEntryPoint) {
        final double result = SpecialBuiltin.callDouble(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()));
        pushDouble(result);
    }

    @INLINE
    public static void indirectCallWord(Address address, CallEntryPoint callEntryPoint) {
        final Word result = SpecialBuiltin.callWord(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()));
        pushWord(result);
    }

    @INLINE
    public static void indirectCallVoid(Address address, CallEntryPoint callEntryPoint, Object receiver) {
        SpecialBuiltin.call(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()), receiver);
    }

    @INLINE
    public static void indirectCallFloat(Address address, CallEntryPoint callEntryPoint, Object receiver) {
        final float result = SpecialBuiltin.callFloat(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()), receiver);
        pushFloat(result);
    }

    @INLINE
    public static void indirectCallLong(Address address, CallEntryPoint callEntryPoint, Object receiver) {
        final long result = SpecialBuiltin.callLong(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()), receiver);
        pushLong(result);
    }

    @INLINE
    public static void indirectCallDouble(Address address, CallEntryPoint callEntryPoint, Object receiver) {
        final double result = SpecialBuiltin.callDouble(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()), receiver);
        pushDouble(result);
    }

    @INLINE
    public static void indirectCallWord(Address address, CallEntryPoint callEntryPoint, Object receiver) {
        final Word result = SpecialBuiltin.callWord(address.plus(CallEntryPoint.JIT_ENTRY_POINT.offset() - callEntryPoint.offset()), receiver);
        pushWord(result);
    }
}
