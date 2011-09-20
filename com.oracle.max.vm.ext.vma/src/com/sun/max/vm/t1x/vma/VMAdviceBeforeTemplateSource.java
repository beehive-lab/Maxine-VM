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
package com.sun.max.vm.t1x.vma;

import static com.oracle.max.vm.ext.t1x.T1XRuntime.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateSource.*;

import com.oracle.max.vm.ext.vma.run.java.*;
import com.oracle.max.vm.ext.vma.runtime.*;
import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.oracle.max.vm.ext.t1x.*;

/**
 * Template source for before advice (where available).
 */
public class VMAdviceBeforeTemplateSource {

// START GENERATED CODE
    @T1X_TEMPLATE(GETFIELD$boolean$resolved)
    public static int getfieldBoolean(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        boolean result = TupleAccess.readBoolean(object, offset);
        return UnsafeCast.asByte(result);
    }

    @T1X_TEMPLATE(GETFIELD$boolean)
    public static int getfieldBoolean(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldBoolean(guard, object);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(object, f.offset());
            postVolatileRead();
            return UnsafeCast.asByte(value);
        } else {
            boolean result = TupleAccess.readBoolean(object, f.offset());
            return UnsafeCast.asByte(result);
        }
    }

    @T1X_TEMPLATE(GETSTATIC$boolean)
    public static int getstaticBoolean(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticBoolean(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticBoolean(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return UnsafeCast.asByte(value);
        } else {
            boolean result = TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
            return UnsafeCast.asByte(result);
        }
    }

    @T1X_TEMPLATE(GETSTATIC$boolean$init)
    public static int getstaticBoolean(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        boolean result = TupleAccess.readBoolean(staticTuple, offset);
        return UnsafeCast.asByte(result);
    }

    @T1X_TEMPLATE(PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(@Slot(1) Object object, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeBoolean(object, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldBoolean(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(object, f.offset(), UnsafeCast.asBoolean((byte) value));
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(object, f.offset(), UnsafeCast.asBoolean((byte) value));
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$boolean$init)
    public static void putstaticBoolean(Object staticTuple, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeBoolean(staticTuple, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticBoolean(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), UnsafeCast.asBoolean((byte) value));
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), UnsafeCast.asBoolean((byte) value));
        }
    }

    @T1X_TEMPLATE(GETFIELD$byte$resolved)
    public static int getfieldByte(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        byte result = TupleAccess.readByte(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$byte)
    public static int getfieldByte(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldByte(guard, object);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldByte(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            byte result = TupleAccess.readByte(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$byte)
    public static int getstaticByte(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticByte(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            byte result = TupleAccess.readByte(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$byte$init)
    public static int getstaticByte(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        byte result = TupleAccess.readByte(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$byte$resolved)
    public static void putfieldByte(@Slot(1) Object object, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeByte(object, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldByte(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(object, f.offset(), (byte) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(object, f.offset(), (byte) value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$byte$init)
    public static void putstaticByte(Object staticTuple, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeByte(staticTuple, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticByte(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), (byte) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), (byte) value);
        }
    }

    @T1X_TEMPLATE(I2B)
    public static int i2b(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(145, value);
        }
        return (byte) value;
    }

    @T1X_TEMPLATE(BALOAD)
    public static int baload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        byte result = ArrayAccess.getByte(array, index);
        return result;
    }

    @T1X_TEMPLATE(BASTORE)
    public static void bastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setByte(array, index, (byte) value);
    }

    @T1X_TEMPLATE(GETFIELD$char$resolved)
    public static int getfieldChar(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        char result = TupleAccess.readChar(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$char)
    public static int getfieldChar(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldChar(guard, object);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldChar(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            char result = TupleAccess.readChar(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$char)
    public static int getstaticChar(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticChar(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            char result = TupleAccess.readChar(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$char$init)
    public static int getstaticChar(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        char result = TupleAccess.readChar(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$char$resolved)
    public static void putfieldChar(@Slot(1) Object object, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeChar(object, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldChar(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(object, f.offset(), (char) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(object, f.offset(), (char) value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$char$init)
    public static void putstaticChar(Object staticTuple, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeChar(staticTuple, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticChar(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), (char) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), (char) value);
        }
    }

    @T1X_TEMPLATE(I2C)
    public static int i2c(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(146, value);
        }
        return (char) value;
    }

    @T1X_TEMPLATE(CALOAD)
    public static int caload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        char result = ArrayAccess.getChar(array, index);
        return result;
    }

    @T1X_TEMPLATE(CASTORE)
    public static void castore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setChar(array, index, (char) value);
    }

    @T1X_TEMPLATE(GETFIELD$short$resolved)
    public static int getfieldShort(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        short result = TupleAccess.readShort(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$short)
    public static int getfieldShort(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldShort(guard, object);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldShort(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            short result = TupleAccess.readShort(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$short)
    public static int getstaticShort(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticShort(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            short result = TupleAccess.readShort(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$short$init)
    public static int getstaticShort(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        short result = TupleAccess.readShort(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$short$resolved)
    public static void putfieldShort(@Slot(1) Object object, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeShort(object, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldShort(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(object, f.offset(), (short) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(object, f.offset(), (short) value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$short$init)
    public static void putstaticShort(Object staticTuple, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeShort(staticTuple, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticShort(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), (short) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), (short) value);
        }
    }

    @T1X_TEMPLATE(I2S)
    public static int i2s(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(147, value);
        }
        return (short) value;
    }

    @T1X_TEMPLATE(SALOAD)
    public static int saload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        short result = ArrayAccess.getShort(array, index);
        return result;
    }

    @T1X_TEMPLATE(SASTORE)
    public static void sastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setShort(array, index, (short) value);
    }

    @T1X_TEMPLATE(GETFIELD$int$resolved)
    public static int getfieldInt(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        int result = TupleAccess.readInt(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$int)
    public static int getfieldInt(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldInt(guard, object);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            int result = TupleAccess.readInt(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$int)
    public static int getstaticInt(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticInt(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            int result = TupleAccess.readInt(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$int$init)
    public static int getstaticInt(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        int result = TupleAccess.readInt(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$int$resolved)
    public static void putfieldInt(@Slot(1) Object object, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeInt(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldInt(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$int$init)
    public static void putstaticInt(Object staticTuple, int offset, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticInt(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(L2I)
    public static int l2i(@Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(136, value);
        }
        return (int) value;
    }

    @T1X_TEMPLATE(F2I)
    public static int f2i(@Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(139, value);
        }
        return T1XRuntime.f2i(value);
    }

    @T1X_TEMPLATE(D2I)
    public static int d2i(@Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(142, value);
        }
        return T1XRuntime.d2i(value);
    }

    @T1X_TEMPLATE(IADD)
    public static int iadd(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(96, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(ISUB)
    public static int isub(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(100, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(IMUL)
    public static int imul(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(104, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(IDIV)
    public static int idiv(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(108, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(IREM)
    public static int irem(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(112, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(INEG)
    public static int ineg(@Slot(0) int value, int zero) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(116, value, zero);
        }
        return zero - value;
    }

    @T1X_TEMPLATE(IOR)
    public static int ior(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(128, value1, value2);
        }
        return value1 | value2;
    }

    @T1X_TEMPLATE(IAND)
    public static int iand(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(126, value1, value2);
        }
        return value1 & value2;
    }

    @T1X_TEMPLATE(IXOR)
    public static int ixor(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(130, value1, value2);
        }
        return value1 ^ value2;
    }

    @T1X_TEMPLATE(ISHL)
    public static int ishl(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(120, value1, value2);
        }
        return value1 << value2;
    }

    @T1X_TEMPLATE(ISHR)
    public static int ishr(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(122, value1, value2);
        }
        return value1 >> value2;
    }

    @T1X_TEMPLATE(IUSHR)
    public static int iushr(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(124, value1, value2);
        }
        return value1 >>> value2;
    }

    @T1X_TEMPLATE(IRETURN)
    @Slot(-1)
    public static int ireturn(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(IRETURN$unlock)
    @Slot(-1)
    public static int ireturnUnlock(Reference object, @Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(IALOAD)
    public static int iaload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        int result = ArrayAccess.getInt(array, index);
        return result;
    }

    @T1X_TEMPLATE(IASTORE)
    public static void iastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setInt(array, index, value);
    }

    @T1X_TEMPLATE(GETFIELD$float$resolved)
    public static float getfieldFloat(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        float result = TupleAccess.readFloat(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$float)
    public static float getfieldFloat(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldFloat(guard, object);
    }

    @NEVER_INLINE
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            float result = TupleAccess.readFloat(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$float)
    public static float getstaticFloat(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticFloat(guard);
    }

    @NEVER_INLINE
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            float result = TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$float$init)
    public static float getstaticFloat(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        float result = TupleAccess.readFloat(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$float$resolved)
    public static void putfieldFloat(@Slot(1) Object object, int offset, @Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeFloat(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) float value) {
        resolveAndPutFieldFloat(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$float$init)
    public static void putstaticFloat(Object staticTuple, int offset, @Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard, @Slot(0) float value) {
        resolveAndPutStaticFloat(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(I2F)
    public static float i2f(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(134, value);
        }
        return value;
    }

    @T1X_TEMPLATE(L2F)
    public static float l2f(@Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(137, value);
        }
        return value;
    }

    @T1X_TEMPLATE(D2F)
    public static float d2f(@Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(144, value);
        }
        return (float) value;
    }

    @T1X_TEMPLATE(FADD)
    public static float fadd(@Slot(1) float value1, @Slot(0) float value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(98, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(FSUB)
    public static float fsub(@Slot(1) float value1, @Slot(0) float value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(102, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(FMUL)
    public static float fmul(@Slot(1) float value1, @Slot(0) float value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(106, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(FDIV)
    public static float fdiv(@Slot(1) float value1, @Slot(0) float value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(110, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(FREM)
    public static float frem(@Slot(1) float value1, @Slot(0) float value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(114, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(FNEG)
    public static float fneg(@Slot(0) float value, float zero) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(118, value, zero);
        }
        return zero - value;
    }

    @T1X_TEMPLATE(FRETURN)
    @Slot(-1)
    public static float freturn(@Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(FRETURN$unlock)
    @Slot(-1)
    public static float freturnUnlock(Reference object, @Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(FALOAD)
    public static float faload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        float result = ArrayAccess.getFloat(array, index);
        return result;
    }

    @T1X_TEMPLATE(FASTORE)
    public static void fastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) float value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setFloat(array, index, value);
    }

    @T1X_TEMPLATE(GETFIELD$long$resolved)
    public static long getfieldLong(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        long result = TupleAccess.readLong(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$long)
    public static long getfieldLong(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldLong(guard, object);
    }

    @NEVER_INLINE
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            long result = TupleAccess.readLong(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$long)
    public static long getstaticLong(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticLong(guard);
    }

    @NEVER_INLINE
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            long result = TupleAccess.readLong(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$long$init)
    public static long getstaticLong(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        long result = TupleAccess.readLong(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$long$resolved)
    public static void putfieldLong(@Slot(2) Object object, int offset, @Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeLong(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) long value) {
        resolveAndPutFieldLong(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$long$init)
    public static void putstaticLong(Object staticTuple, int offset, @Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard, @Slot(0) long value) {
        resolveAndPutStaticLong(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(I2L)
    public static long i2l(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(133, value);
        }
        return value;
    }

    @T1X_TEMPLATE(F2L)
    public static long f2l(@Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(140, value);
        }
        return T1XRuntime.f2l(value);
    }

    @T1X_TEMPLATE(D2L)
    public static long d2l(@Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(143, value);
        }
        return T1XRuntime.d2l(value);
    }

    @T1X_TEMPLATE(LADD)
    public static long ladd(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(97, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(LSUB)
    public static long lsub(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(101, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(LMUL)
    public static long lmul(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(105, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(LDIV)
    public static long ldiv(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(109, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(LREM)
    public static long lrem(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(113, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(LNEG)
    public static long lneg(@Slot(0) long value, long zero) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(117, value, zero);
        }
        return zero - value;
    }

    @T1X_TEMPLATE(LOR)
    public static long lor(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(129, value1, value2);
        }
        return value1 | value2;
    }

    @T1X_TEMPLATE(LAND)
    public static long land(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(127, value1, value2);
        }
        return value1 & value2;
    }

    @T1X_TEMPLATE(LXOR)
    public static long lxor(@Slot(2) long value1, @Slot(0) long value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(131, value1, value2);
        }
        return value1 ^ value2;
    }

    @T1X_TEMPLATE(LSHL)
    public static long lshl(@Slot(1) long value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(121, value1, value2);
        }
        return value1 << value2;
    }

    @T1X_TEMPLATE(LSHR)
    public static long lshr(@Slot(1) long value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(123, value1, value2);
        }
        return value1 >> value2;
    }

    @T1X_TEMPLATE(LUSHR)
    public static long lushr(@Slot(1) long value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(125, value1, value2);
        }
        return value1 >>> value2;
    }

    @T1X_TEMPLATE(LRETURN)
    @Slot(-1)
    public static long lreturn(@Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(LRETURN$unlock)
    @Slot(-1)
    public static long lreturnUnlock(Reference object, @Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(LALOAD)
    public static long laload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        long result = ArrayAccess.getLong(array, index);
        return result;
    }

    @T1X_TEMPLATE(LASTORE)
    public static void lastore(@Slot(3) Object array, @Slot(2) int index, @Slot(0) long value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setLong(array, index, value);
    }

    @T1X_TEMPLATE(GETFIELD$double$resolved)
    public static double getfieldDouble(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        double result = TupleAccess.readDouble(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$double)
    public static double getfieldDouble(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldDouble(guard, object);
    }

    @NEVER_INLINE
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            double result = TupleAccess.readDouble(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$double)
    public static double getstaticDouble(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticDouble(guard);
    }

    @NEVER_INLINE
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            double result = TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$double$init)
    public static double getstaticDouble(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        double result = TupleAccess.readDouble(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$double$resolved)
    public static void putfieldDouble(@Slot(2) Object object, int offset, @Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.writeDouble(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) double value) {
        resolveAndPutFieldDouble(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$double$init)
    public static void putstaticDouble(Object staticTuple, int offset, @Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard, @Slot(0) double value) {
        resolveAndPutStaticDouble(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(I2D)
    public static double i2d(@Slot(0) int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(135, value);
        }
        return value;
    }

    @T1X_TEMPLATE(L2D)
    public static double l2d(@Slot(0) long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(138, value);
        }
        return value;
    }

    @T1X_TEMPLATE(F2D)
    public static double f2d(@Slot(0) float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(141, value);
        }
        return value;
    }

    @T1X_TEMPLATE(DADD)
    public static double dadd(@Slot(2) double value1, @Slot(0) double value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(99, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(DSUB)
    public static double dsub(@Slot(2) double value1, @Slot(0) double value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(103, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(DMUL)
    public static double dmul(@Slot(2) double value1, @Slot(0) double value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(107, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(DDIV)
    public static double ddiv(@Slot(2) double value1, @Slot(0) double value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(111, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(DREM)
    public static double drem(@Slot(2) double value1, @Slot(0) double value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(115, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(DNEG)
    public static double dneg(@Slot(0) double value, double zero) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(119, value, zero);
        }
        return zero - value;
    }

    @T1X_TEMPLATE(DRETURN)
    @Slot(-1)
    public static double dreturn(@Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(DRETURN$unlock)
    @Slot(-1)
    public static double dreturnUnlock(Reference object, @Slot(0) double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(DALOAD)
    public static double daload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        double result = ArrayAccess.getDouble(array, index);
        return result;
    }

    @T1X_TEMPLATE(DASTORE)
    public static void dastore(@Slot(3) Object array, @Slot(2) int index, @Slot(0) double value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setDouble(array, index, value);
    }

    @T1X_TEMPLATE(GETFIELD$reference$resolved)
    public static Reference getfieldObject(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        Object result = TupleAccess.readObject(object, offset);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(GETFIELD$reference)
    public static Reference getfieldReference(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldReference(guard, object);
    }

    @NEVER_INLINE
    public static Reference resolveAndGetFieldReference(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(object, f.offset());
            postVolatileRead();
            return Reference.fromJava(value);
        } else {
            Object result = TupleAccess.readObject(object, f.offset());
            return Reference.fromJava(result);
        }
    }

    @T1X_TEMPLATE(GETSTATIC$reference)
    public static Reference getstaticReference(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticReference(guard);
    }

    @NEVER_INLINE
    public static Reference resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return Reference.fromJava(value);
        } else {
            Object result = TupleAccess.readObject(f.holder().staticTuple(), f.offset());
            return Reference.fromJava(result);
        }
    }

    @T1X_TEMPLATE(GETSTATIC$reference$init)
    public static Reference getstaticObject(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        Object result = TupleAccess.readObject(staticTuple, offset);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(PUTFIELD$reference$resolved)
    public static void putfieldReference(@Slot(1) Object object, int offset, @Slot(0) Reference value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Reference value) {
        resolveAndPutFieldReference(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Reference value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$reference$init)
    public static void putstaticReference(Object staticTuple, int offset, @Slot(0) Reference value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard, @Slot(0) Reference value) {
        resolveAndPutStaticReference(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Reference value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(ARETURN)
    @Slot(-1)
    public static Reference areturn(@Slot(0) Reference value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(ARETURN$unlock)
    @Slot(-1)
    public static Reference areturnUnlock(Reference object, @Slot(0) Reference value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value);
        }
        return value;
    }

    @T1X_TEMPLATE(AALOAD)
    public static Reference aaload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index);
        }
        Object result = ArrayAccess.getObject(array, index);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(AASTORE)
    public static void aastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) Reference value) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    @T1X_TEMPLATE(GETFIELD$word$resolved)
    public static Word getfieldWord(@Slot(0) Object object, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset);
        }
        Word result = TupleAccess.readWord(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$word)
    public static Word getfieldWord(ResolutionGuard.InPool guard, @Slot(0) Object object) {
        return resolveAndGetFieldWord(guard, object);
    }

    @NEVER_INLINE
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            Word result = TupleAccess.readWord(object, f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$word)
    public static Word getstaticWord(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticWord(guard);
    }

    @NEVER_INLINE
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset());
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            Word result = TupleAccess.readWord(f.holder().staticTuple(), f.offset());
            return result;
        }
    }

    @T1X_TEMPLATE(GETSTATIC$word$init)
    public static Word getstaticWord(Object staticTuple, int offset) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset);
        }
        Word result = TupleAccess.readWord(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$word$resolved)
    public static void putfieldWord(@Slot(1) Object object, int offset, @Slot(0) Word value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value.asAddress().toLong());
        }
        TupleAccess.writeWord(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Word value) {
        resolveAndPutFieldWord(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value.asAddress().toLong());
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$word$init)
    public static void putstaticWord(Object staticTuple, int offset, @Slot(0) Word value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value.asAddress().toLong());
        }
        TupleAccess.writeWord(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard, @Slot(0) Word value) {
        resolveAndPutStaticWord(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value.asAddress().toLong());
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(WRETURN)
    @Slot(-1)
    public static Word wreturn(@Slot(0) Word value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value.asAddress().toLong());
        }
        return value;
    }

    @T1X_TEMPLATE(WRETURN$unlock)
    @Slot(-1)
    public static Word wreturnUnlock(Reference object, @Slot(0) Word value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(value.asAddress().toLong());
        }
        return value;
    }

    @T1X_TEMPLATE(RETURN)
    @Slot(-1)
    public static void vreturn() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn();
        }
    }

    @T1X_TEMPLATE(RETURN$unlock)
    @Slot(-1)
    public static void vreturnUnlock(Reference object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn();
        }
    }

    @T1X_TEMPLATE(LCMP)
    public static int lcmp(@Slot(2) long value1, @Slot(0) long value2) {
        int result = rawCompare(Bytecodes.LCMP, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(148, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(FCMPL)
    public static int fcmpl(@Slot(1) float value1, @Slot(0) float value2) {
        int result = rawCompare(Bytecodes.FCMPL, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(149, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(FCMPG)
    public static int fcmpg(@Slot(1) float value1, @Slot(0) float value2) {
        int result = rawCompare(Bytecodes.FCMPG, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(150, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(DCMPL)
    public static int dcmpl(@Slot(2) double value1, @Slot(0) double value2) {
        int result = rawCompare(Bytecodes.DCMPL, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(151, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(DCMPG)
    public static int dcmpg(@Slot(2) double value1, @Slot(0) double value2) {
        int result = rawCompare(Bytecodes.DCMPG, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(152, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(CHECKCAST)
    public static Object checkcast(ResolutionGuard guard, @Slot(0) Object object) {
        resolveAndCheckcast(guard, object);
        return object;
    }

    @T1X_TEMPLATE(CHECKCAST$resolved)
    public static Object checkcast(ClassActor classActor, @Slot(0) Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeCheckCast(object, classActor);
        }
        Snippets.checkCast(classActor, object);
        return object;
    }

    @NEVER_INLINE
    private static void resolveAndCheckcast(ResolutionGuard guard, final Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeCheckCast(object, classActor);
        }
        Snippets.checkCast(classActor, object);
    }

    @T1X_TEMPLATE(ARRAYLENGTH)
    public static int arraylength(@Slot(0) Object array) {
        int length = ArrayAccess.readArrayLength(array);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLength(array, length);
        }
        return length;
    }

    @T1X_TEMPLATE(ATHROW)
    public static void athrow(@Slot(0) Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeThrow(object);
        }
        Throw.raise(object);
    }

    @T1X_TEMPLATE(MONITORENTER)
    public static void monitorenter(@Slot(0) Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorEnter(object);
        }
        Monitor.enter(object);
    }

    @T1X_TEMPLATE(MONITOREXIT)
    public static void monitorexit(@Slot(0) Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.exit(object);
    }

    @T1X_TEMPLATE(INSTANCEOF)
    public static int instanceof_(ResolutionGuard guard, @Slot(0) Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInstanceOf(object, classActor);
        }
        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));
    }

    @T1X_TEMPLATE(INSTANCEOF$resolved)
    public static int instanceof_(ClassActor classActor, @Slot(0) Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInstanceOf(object, classActor);
        }
        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));
    }

    @T1X_TEMPLATE(RETURN$registerFinalizer)
    @Slot(-1)
    public static void vreturnRegisterFinalizer(Reference object) {
        if (ObjectAccess.readClassActor(object).hasFinalizer()) {
            SpecialReferenceManager.registerFinalizee(object);
        }
    }

    @T1X_TEMPLATE(LOCK)
    public static void lock(Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorEnter(object);
        }
        Monitor.enter(object);
    }

    @T1X_TEMPLATE(UNLOCK)
    public static void unlock(Object object) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(object);
        }
        Monitor.exit(object);
    }

    @T1X_TEMPLATE(POP)
    public static void pop() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(87);
        }
    }

    @T1X_TEMPLATE(POP2)
    public static void pop2() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(88);
        }
    }

    @T1X_TEMPLATE(DUP)
    public static void dup() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(89);
        }
    }

    @T1X_TEMPLATE(DUP_X1)
    public static void dup_x1() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(90);
        }
    }

    @T1X_TEMPLATE(DUP_X2)
    public static void dup_x2() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(91);
        }
    }

    @T1X_TEMPLATE(DUP2)
    public static void dup2() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(92);
        }
    }

    @T1X_TEMPLATE(DUP2_X1)
    public static void dup2_x1() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(93);
        }
    }

    @T1X_TEMPLATE(DUP2_X2)
    public static void dup2_x2() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(94);
        }
    }

    @T1X_TEMPLATE(SWAP)
    public static void swap() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(95);
        }
    }

    @T1X_TEMPLATE(ACONST_NULL)
    public static void aconst_null() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(null);
        }
    }

    @T1X_TEMPLATE(ICONST)
    public static void iconst(int constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LCONST)
    public static void lconst(long constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(FCONST)
    public static void fconst(float constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(DCONST)
    public static void dconst(double constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LDC$int)
    public static void ildc(int constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LDC$long)
    public static void lldc(long constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LDC$float)
    public static void fldc(float constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LDC$double)
    public static void dldc(double constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LDC$reference)
    public static void uoldc(ResolutionGuard guard) {
        ClassActor classActor = Snippets.resolveClass(guard);
        Object constant = classActor.javaClass();
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(LDC$reference$resolved)
    public static void oldc(Object constant) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(constant);
        }
    }

    @T1X_TEMPLATE(ILOAD)
    public static void iload(int index) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(index);
        }
    }

    @T1X_TEMPLATE(LLOAD)
    public static void lload(int index) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(index);
        }
    }

    @T1X_TEMPLATE(FLOAD)
    public static void fload(int index) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(index);
        }
    }

    @T1X_TEMPLATE(DLOAD)
    public static void dload(int index) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(index);
        }
    }

    @T1X_TEMPLATE(ALOAD)
    public static void oload(int index) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(index);
        }
    }

    @T1X_TEMPLATE(WLOAD)
    public static void wload(int index) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(index);
        }
    }

    @T1X_TEMPLATE(ISTORE)
    public static void istore(int index, int value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(index, value);
        }
    }

    @T1X_TEMPLATE(LSTORE)
    public static void lstore(int index, long value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(index, value);
        }
    }

    @T1X_TEMPLATE(FSTORE)
    public static void fstore(int index, float value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(index, value);
        }
    }

    @T1X_TEMPLATE(DSTORE)
    public static void dstore(int index, double value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(index, value);
        }
    }

    @T1X_TEMPLATE(ASTORE)
    public static void ostore(int index, Object value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(index, value);
        }
    }

    @T1X_TEMPLATE(WSTORE)
    public static void wstore(int index, Word value) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(index, value.asAddress().toLong());
        }
    }

    @T1X_TEMPLATE(IINC)
    public static void iinc(int index, int increment) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(132, index, increment);
        }
    }

    @T1X_TEMPLATE(IFEQ)
    public static void ifeq(@Slot(0) int value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(153, value1, 0);
        }
    }

    @T1X_TEMPLATE(IFNE)
    public static void ifne(@Slot(0) int value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(154, value1, 0);
        }
    }

    @T1X_TEMPLATE(IFLT)
    public static void iflt(@Slot(0) int value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(155, value1, 0);
        }
    }

    @T1X_TEMPLATE(IFGE)
    public static void ifge(@Slot(0) int value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(156, value1, 0);
        }
    }

    @T1X_TEMPLATE(IFGT)
    public static void ifgt(@Slot(0) int value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(157, value1, 0);
        }
    }

    @T1X_TEMPLATE(IFLE)
    public static void ifle(@Slot(0) int value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(158, value1, 0);
        }
    }

    @T1X_TEMPLATE(IFNULL)
    public static void ifnull(@Slot(0) Object value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(198, value1, null);
        }
    }

    @T1X_TEMPLATE(IFNONNULL)
    public static void ifnonnull(@Slot(0) Object value1) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(199, value1, null);
        }
    }

    @T1X_TEMPLATE(IF_ICMPEQ)
    public static void if_icmpeq(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(159, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ICMPNE)
    public static void if_icmpne(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(160, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ICMPLT)
    public static void if_icmplt(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(161, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ICMPGE)
    public static void if_icmpge(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(162, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ICMPGT)
    public static void if_icmpgt(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(163, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ICMPLE)
    public static void if_icmple(@Slot(1) int value1, @Slot(0) int value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(164, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ACMPEQ)
    public static void if_acmpeq(@Slot(1) Object value1, @Slot(0) Object value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(165, value1, value2);
        }
    }

    @T1X_TEMPLATE(IF_ACMPNE)
    public static void if_acmpne(@Slot(1) Object value1, @Slot(0) Object value2) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(166, value1, value2);
        }
    }

    @T1X_TEMPLATE(GOTO)
    public static void goto_s() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeBytecode(167);
        }
    }

    @T1X_TEMPLATE(GOTO_W)
    public static void goto_w() {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeBytecode(200);
        }
    }

// END GENERATED CODE

}
