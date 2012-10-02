/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x.vma;

import static com.oracle.max.vm.ext.t1x.T1XRuntime.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateSource.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;

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
import com.sun.max.vm.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.oracle.max.vm.ext.t1x.*;

/**
 * Template source for before advice (where available).
 */
public class VMAdviceBeforeTemplateSource {

// START GENERATED CODE
    @T1X_TEMPLATE(GETFIELD$boolean$resolved)
    public static int getfieldBoolean(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        boolean result = TupleAccess.readBoolean(object, offset);
        return UnsafeCast.asByte(result);
    }

    @T1X_TEMPLATE(GETFIELD$boolean)
    public static int getfieldBoolean(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldBoolean(guard, object, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static int getstaticBoolean(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticBoolean(guard, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticBoolean(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static int getstaticBoolean(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        boolean result = TupleAccess.readBoolean(staticTuple, offset);
        return UnsafeCast.asByte(result);
    }

    @T1X_TEMPLATE(PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(@Slot(1) Object object, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeBoolean(object, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value, int bci) {
        resolveAndPutFieldBoolean(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, int value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticBoolean(Object staticTuple, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeBoolean(staticTuple, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard, @Slot(0) int value, int bci) {
        resolveAndPutStaticBoolean(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, int value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static int getfieldByte(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        byte result = TupleAccess.readByte(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$byte)
    public static int getfieldByte(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldByte(guard, object, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldByte(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static int getstaticByte(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticByte(guard, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticByte(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static int getstaticByte(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        byte result = TupleAccess.readByte(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$byte$resolved)
    public static void putfieldByte(@Slot(1) Object object, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeByte(object, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value, int bci) {
        resolveAndPutFieldByte(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, int value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticByte(Object staticTuple, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeByte(staticTuple, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard, @Slot(0) int value, int bci) {
        resolveAndPutStaticByte(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, int value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static int i2b(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 145, value);
        }
        return (byte) value;
    }

    @T1X_TEMPLATE(BALOAD)
    public static int baload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        byte result = ArrayAccess.getByte(array, index);
        return result;
    }

    @T1X_TEMPLATE(BASTORE)
    public static void bastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setByte(array, index, (byte) value);
    }

    @T1X_TEMPLATE(GETFIELD$char$resolved)
    public static int getfieldChar(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        char result = TupleAccess.readChar(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$char)
    public static int getfieldChar(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldChar(guard, object, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldChar(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static int getstaticChar(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticChar(guard, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticChar(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static int getstaticChar(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        char result = TupleAccess.readChar(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$char$resolved)
    public static void putfieldChar(@Slot(1) Object object, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeChar(object, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value, int bci) {
        resolveAndPutFieldChar(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, int value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticChar(Object staticTuple, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeChar(staticTuple, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard, @Slot(0) int value, int bci) {
        resolveAndPutStaticChar(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, int value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static int i2c(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 146, value);
        }
        return (char) value;
    }

    @T1X_TEMPLATE(CALOAD)
    public static int caload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        char result = ArrayAccess.getChar(array, index);
        return result;
    }

    @T1X_TEMPLATE(CASTORE)
    public static void castore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setChar(array, index, (char) value);
    }

    @T1X_TEMPLATE(GETFIELD$short$resolved)
    public static int getfieldShort(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        short result = TupleAccess.readShort(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$short)
    public static int getfieldShort(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldShort(guard, object, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldShort(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static int getstaticShort(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticShort(guard, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticShort(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static int getstaticShort(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        short result = TupleAccess.readShort(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$short$resolved)
    public static void putfieldShort(@Slot(1) Object object, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeShort(object, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value, int bci) {
        resolveAndPutFieldShort(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, int value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticShort(Object staticTuple, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeShort(staticTuple, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard, @Slot(0) int value, int bci) {
        resolveAndPutStaticShort(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, int value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static int i2s(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 147, value);
        }
        return (short) value;
    }

    @T1X_TEMPLATE(SALOAD)
    public static int saload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        short result = ArrayAccess.getShort(array, index);
        return result;
    }

    @T1X_TEMPLATE(SASTORE)
    public static void sastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setShort(array, index, (short) value);
    }

    @T1X_TEMPLATE(GETFIELD$int$resolved)
    public static int getfieldInt(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        int result = TupleAccess.readInt(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$int)
    public static int getfieldInt(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldInt(guard, object, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static int getstaticInt(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticInt(guard, bci);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static int getstaticInt(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        int result = TupleAccess.readInt(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$int$resolved)
    public static void putfieldInt(@Slot(1) Object object, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeInt(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value, int bci) {
        resolveAndPutFieldInt(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticInt(Object staticTuple, int offset, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard, @Slot(0) int value, int bci) {
        resolveAndPutStaticInt(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static int l2i(@Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 136, value);
        }
        return (int) value;
    }

    @T1X_TEMPLATE(F2I)
    public static int f2i(@Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 139, value);
        }
        return T1XRuntime.f2i(value);
    }

    @T1X_TEMPLATE(D2I)
    public static int d2i(@Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 142, value);
        }
        return T1XRuntime.d2i(value);
    }

    @T1X_TEMPLATE(IADD)
    public static int iadd(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 96, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(ISUB)
    public static int isub(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 100, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(IMUL)
    public static int imul(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 104, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(IDIV)
    public static int idiv(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 108, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(IREM)
    public static int irem(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 112, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(INEG)
    public static int ineg(@Slot(0) int value, int zero, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 116, value, zero);
        }
        return zero - value;
    }

    @T1X_TEMPLATE(IOR)
    public static int ior(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 128, value1, value2);
        }
        return value1 | value2;
    }

    @T1X_TEMPLATE(IAND)
    public static int iand(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 126, value1, value2);
        }
        return value1 & value2;
    }

    @T1X_TEMPLATE(IXOR)
    public static int ixor(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 130, value1, value2);
        }
        return value1 ^ value2;
    }

    @T1X_TEMPLATE(ISHL)
    public static int ishl(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 120, value1, value2);
        }
        return value1 << value2;
    }

    @T1X_TEMPLATE(ISHR)
    public static int ishr(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 122, value1, value2);
        }
        return value1 >> value2;
    }

    @T1X_TEMPLATE(IUSHR)
    public static int iushr(@Slot(1) int value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 124, value1, value2);
        }
        return value1 >>> value2;
    }

    @T1X_TEMPLATE(IRETURN)
    @Slot(-1)
    public static int ireturn(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(IRETURN$unlock)
    @Slot(-1)
    public static int ireturnUnlock(Reference object, @Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(IALOAD)
    public static int iaload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        int result = ArrayAccess.getInt(array, index);
        return result;
    }

    @T1X_TEMPLATE(IASTORE)
    public static void iastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setInt(array, index, value);
    }

    @T1X_TEMPLATE(GETFIELD$float$resolved)
    public static float getfieldFloat(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        float result = TupleAccess.readFloat(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$float)
    public static float getfieldFloat(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldFloat(guard, object, bci);
    }

    @NEVER_INLINE
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static float getstaticFloat(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticFloat(guard, bci);
    }

    @NEVER_INLINE
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static float getstaticFloat(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        float result = TupleAccess.readFloat(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$float$resolved)
    public static void putfieldFloat(@Slot(1) Object object, int offset, @Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeFloat(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) float value, int bci) {
        resolveAndPutFieldFloat(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticFloat(Object staticTuple, int offset, @Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard, @Slot(0) float value, int bci) {
        resolveAndPutStaticFloat(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static float i2f(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 134, value);
        }
        return value;
    }

    @T1X_TEMPLATE(L2F)
    public static float l2f(@Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 137, value);
        }
        return value;
    }

    @T1X_TEMPLATE(D2F)
    public static float d2f(@Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 144, value);
        }
        return (float) value;
    }

    @T1X_TEMPLATE(FADD)
    public static float fadd(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 98, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(FSUB)
    public static float fsub(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 102, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(FMUL)
    public static float fmul(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 106, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(FDIV)
    public static float fdiv(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 110, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(FREM)
    public static float frem(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 114, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(FNEG)
    public static float fneg(@Slot(0) float value, float zero, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 118, value, zero);
        }
        float res;
        if (Float.floatToRawIntBits(value) == Float.floatToRawIntBits(zero)) {
            res = -0.0f;
        } else {
            res = zero - value;
        }
        return res;
    }

    @T1X_TEMPLATE(FRETURN)
    @Slot(-1)
    public static float freturn(@Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(FRETURN$unlock)
    @Slot(-1)
    public static float freturnUnlock(Reference object, @Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(FALOAD)
    public static float faload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        float result = ArrayAccess.getFloat(array, index);
        return result;
    }

    @T1X_TEMPLATE(FASTORE)
    public static void fastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) float value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setFloat(array, index, value);
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$float)
    @Slot(-1)
    public static Address invokevirtualFloat(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$float$resolved)
    @Slot(-1)
    public static Address invokevirtualFloat(VirtualMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$float$instrumented)
    @Slot(-1)
    public static Address invokevirtualFloat(VirtualMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$float)
    @Slot(-1)
    public static Address invokeinterfaceFloat(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$float$resolved)
    @Slot(-1)
    public static Address invokeinterfaceFloat(InterfaceMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$float$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceFloat(InterfaceMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$float)
    @Slot(-1)
    public static Address invokespecialFloat(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        VirtualMethodActor methodActor = VMAT1XRuntime.resolveSpecialMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.initializeSpecialMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESPECIAL$float$resolved)
    public static void invokespecialFloat(VirtualMethodActor methodActor, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKESTATIC$float)
    @Slot(-1)
    public static Address invokestaticFloat(ResolutionGuard.InPool guard, int bci) {
        StaticMethodActor methodActor = VMAT1XRuntime.resolveStaticMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
        return VMAT1XRuntime.initializeStaticMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESTATIC$float$init)
    public static void invokestaticFloat(StaticMethodActor methodActor, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
    }

    @T1X_TEMPLATE(GETFIELD$long$resolved)
    public static long getfieldLong(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        long result = TupleAccess.readLong(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$long)
    public static long getfieldLong(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldLong(guard, object, bci);
    }

    @NEVER_INLINE
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static long getstaticLong(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticLong(guard, bci);
    }

    @NEVER_INLINE
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static long getstaticLong(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        long result = TupleAccess.readLong(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$long$resolved)
    public static void putfieldLong(@Slot(2) Object object, int offset, @Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeLong(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) long value, int bci) {
        resolveAndPutFieldLong(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticLong(Object staticTuple, int offset, @Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard, @Slot(0) long value, int bci) {
        resolveAndPutStaticLong(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static long i2l(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 133, value);
        }
        return value;
    }

    @T1X_TEMPLATE(F2L)
    public static long f2l(@Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 140, value);
        }
        return T1XRuntime.f2l(value);
    }

    @T1X_TEMPLATE(D2L)
    public static long d2l(@Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 143, value);
        }
        return T1XRuntime.d2l(value);
    }

    @T1X_TEMPLATE(LADD)
    public static long ladd(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 97, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(LSUB)
    public static long lsub(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 101, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(LMUL)
    public static long lmul(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 105, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(LDIV)
    public static long ldiv(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 109, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(LREM)
    public static long lrem(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 113, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(LNEG)
    public static long lneg(@Slot(0) long value, long zero, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 117, value, zero);
        }
        return zero - value;
    }

    @T1X_TEMPLATE(LOR)
    public static long lor(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 129, value1, value2);
        }
        return value1 | value2;
    }

    @T1X_TEMPLATE(LAND)
    public static long land(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 127, value1, value2);
        }
        return value1 & value2;
    }

    @T1X_TEMPLATE(LXOR)
    public static long lxor(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 131, value1, value2);
        }
        return value1 ^ value2;
    }

    @T1X_TEMPLATE(LSHL)
    public static long lshl(@Slot(1) long value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 121, value1, value2);
        }
        return value1 << value2;
    }

    @T1X_TEMPLATE(LSHR)
    public static long lshr(@Slot(1) long value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 123, value1, value2);
        }
        return value1 >> value2;
    }

    @T1X_TEMPLATE(LUSHR)
    public static long lushr(@Slot(1) long value1, @Slot(0) int value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 125, value1, value2);
        }
        return value1 >>> value2;
    }

    @T1X_TEMPLATE(LRETURN)
    @Slot(-1)
    public static long lreturn(@Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(LRETURN$unlock)
    @Slot(-1)
    public static long lreturnUnlock(Reference object, @Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(LALOAD)
    public static long laload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        long result = ArrayAccess.getLong(array, index);
        return result;
    }

    @T1X_TEMPLATE(LASTORE)
    public static void lastore(@Slot(3) Object array, @Slot(2) int index, @Slot(0) long value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setLong(array, index, value);
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$long)
    @Slot(-1)
    public static Address invokevirtualLong(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$long$resolved)
    @Slot(-1)
    public static Address invokevirtualLong(VirtualMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$long$instrumented)
    @Slot(-1)
    public static Address invokevirtualLong(VirtualMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$long)
    @Slot(-1)
    public static Address invokeinterfaceLong(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$long$resolved)
    @Slot(-1)
    public static Address invokeinterfaceLong(InterfaceMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$long$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceLong(InterfaceMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$long)
    @Slot(-1)
    public static Address invokespecialLong(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        VirtualMethodActor methodActor = VMAT1XRuntime.resolveSpecialMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.initializeSpecialMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESPECIAL$long$resolved)
    public static void invokespecialLong(VirtualMethodActor methodActor, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKESTATIC$long)
    @Slot(-1)
    public static Address invokestaticLong(ResolutionGuard.InPool guard, int bci) {
        StaticMethodActor methodActor = VMAT1XRuntime.resolveStaticMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
        return VMAT1XRuntime.initializeStaticMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESTATIC$long$init)
    public static void invokestaticLong(StaticMethodActor methodActor, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
    }

    @T1X_TEMPLATE(GETFIELD$double$resolved)
    public static double getfieldDouble(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        double result = TupleAccess.readDouble(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$double)
    public static double getfieldDouble(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldDouble(guard, object, bci);
    }

    @NEVER_INLINE
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static double getstaticDouble(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticDouble(guard, bci);
    }

    @NEVER_INLINE
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static double getstaticDouble(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        double result = TupleAccess.readDouble(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$double$resolved)
    public static void putfieldDouble(@Slot(2) Object object, int offset, @Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.writeDouble(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) double value, int bci) {
        resolveAndPutFieldDouble(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticDouble(Object staticTuple, int offset, @Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard, @Slot(0) double value, int bci) {
        resolveAndPutStaticDouble(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static double i2d(@Slot(0) int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 135, value);
        }
        return value;
    }

    @T1X_TEMPLATE(L2D)
    public static double l2d(@Slot(0) long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 138, value);
        }
        return value;
    }

    @T1X_TEMPLATE(F2D)
    public static double f2d(@Slot(0) float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConversion(bci, 141, value);
        }
        return value;
    }

    @T1X_TEMPLATE(DADD)
    public static double dadd(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 99, value1, value2);
        }
        return value1 + value2;
    }

    @T1X_TEMPLATE(DSUB)
    public static double dsub(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 103, value1, value2);
        }
        return value1 - value2;
    }

    @T1X_TEMPLATE(DMUL)
    public static double dmul(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 107, value1, value2);
        }
        return value1 * value2;
    }

    @T1X_TEMPLATE(DDIV)
    public static double ddiv(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 111, value1, value2);
        }
        return value1 / value2;
    }

    @T1X_TEMPLATE(DREM)
    public static double drem(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 115, value1, value2);
        }
        return value1 % value2;
    }

    @T1X_TEMPLATE(DNEG)
    public static double dneg(@Slot(0) double value, double zero, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 119, value, zero);
        }
        double res;
        if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(zero)) {
            res = -0.0d;
        } else {
            res = zero - value;
        }
        return res;
    }

    @T1X_TEMPLATE(DRETURN)
    @Slot(-1)
    public static double dreturn(@Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(DRETURN$unlock)
    @Slot(-1)
    public static double dreturnUnlock(Reference object, @Slot(0) double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(DALOAD)
    public static double daload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        double result = ArrayAccess.getDouble(array, index);
        return result;
    }

    @T1X_TEMPLATE(DASTORE)
    public static void dastore(@Slot(3) Object array, @Slot(2) int index, @Slot(0) double value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.setDouble(array, index, value);
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$double)
    @Slot(-1)
    public static Address invokevirtualDouble(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$double$resolved)
    @Slot(-1)
    public static Address invokevirtualDouble(VirtualMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$double$instrumented)
    @Slot(-1)
    public static Address invokevirtualDouble(VirtualMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$double)
    @Slot(-1)
    public static Address invokeinterfaceDouble(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$double$resolved)
    @Slot(-1)
    public static Address invokeinterfaceDouble(InterfaceMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$double$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceDouble(InterfaceMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$double)
    @Slot(-1)
    public static Address invokespecialDouble(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        VirtualMethodActor methodActor = VMAT1XRuntime.resolveSpecialMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.initializeSpecialMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESPECIAL$double$resolved)
    public static void invokespecialDouble(VirtualMethodActor methodActor, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKESTATIC$double)
    @Slot(-1)
    public static Address invokestaticDouble(ResolutionGuard.InPool guard, int bci) {
        StaticMethodActor methodActor = VMAT1XRuntime.resolveStaticMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
        return VMAT1XRuntime.initializeStaticMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESTATIC$double$init)
    public static void invokestaticDouble(StaticMethodActor methodActor, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
    }

    @T1X_TEMPLATE(GETFIELD$reference$resolved)
    public static Reference getfieldObject(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        Object result = TupleAccess.readObject(object, offset);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(GETFIELD$reference)
    public static Reference getfieldReference(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldReference(guard, object, bci);
    }

    @NEVER_INLINE
    public static Reference resolveAndGetFieldReference(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static Reference getstaticReference(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticReference(guard, bci);
    }

    @NEVER_INLINE
    public static Reference resolveAndGetStaticReference(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static Reference getstaticObject(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        Object result = TupleAccess.readObject(staticTuple, offset);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(PUTFIELD$reference$resolved)
    public static void putfieldReference(@Slot(1) Object object, int offset, @Slot(0) Reference value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value);
        }
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Reference value, int bci) {
        resolveAndPutFieldReference(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Reference value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value);
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
    public static void putstaticReference(Object staticTuple, int offset, @Slot(0) Reference value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value);
        }
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard, @Slot(0) Reference value, int bci) {
        resolveAndPutStaticReference(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Reference value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value);
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
    public static Reference areturn(@Slot(0) Reference value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(ARETURN$unlock)
    @Slot(-1)
    public static Reference areturnUnlock(Reference object, @Slot(0) Reference value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci, value);
        }
        return value;
    }

    @T1X_TEMPLATE(AALOAD)
    public static Reference aaload(@Slot(1) Object array, @Slot(0) int index, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(bci, array, index);
        }
        Object result = ArrayAccess.getObject(array, index);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(AASTORE)
    public static void aastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) Reference value, int bci) {
        ArrayAccess.checkIndex(array, index);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(bci, array, index, value);
        }
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$reference)
    @Slot(-1)
    public static Address invokevirtualObject(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$reference$resolved)
    @Slot(-1)
    public static Address invokevirtualObject(VirtualMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$reference$instrumented)
    @Slot(-1)
    public static Address invokevirtualObject(VirtualMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$reference)
    @Slot(-1)
    public static Address invokeinterfaceObject(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$reference$resolved)
    @Slot(-1)
    public static Address invokeinterfaceObject(InterfaceMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$reference$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceObject(InterfaceMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$reference)
    @Slot(-1)
    public static Address invokespecialObject(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        VirtualMethodActor methodActor = VMAT1XRuntime.resolveSpecialMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.initializeSpecialMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESPECIAL$reference$resolved)
    public static void invokespecialObject(VirtualMethodActor methodActor, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKESTATIC$reference)
    @Slot(-1)
    public static Address invokestaticObject(ResolutionGuard.InPool guard, int bci) {
        StaticMethodActor methodActor = VMAT1XRuntime.resolveStaticMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
        return VMAT1XRuntime.initializeStaticMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESTATIC$reference$init)
    public static void invokestaticObject(StaticMethodActor methodActor, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
    }

    @T1X_TEMPLATE(GETFIELD$word$resolved)
    public static Word getfieldWord(@Slot(0) Object object, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, offset);
        }
        Word result = TupleAccess.readWord(object, offset);
        return result;
    }

    @T1X_TEMPLATE(GETFIELD$word)
    public static Word getfieldWord(ResolutionGuard.InPool guard, @Slot(0) Object object, int bci) {
        return resolveAndGetFieldWord(guard, object, bci);
    }

    @NEVER_INLINE
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, Object object, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(bci, object, f.offset());
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
    public static Word getstaticWord(ResolutionGuard.InPool guard, int bci) {
        return resolveAndGetStaticWord(guard, bci);
    }

    @NEVER_INLINE
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, f.holder().staticTuple(), f.offset());
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
    public static Word getstaticWord(Object staticTuple, int offset, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(bci, staticTuple, offset);
        }
        Word result = TupleAccess.readWord(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$word$resolved)
    public static void putfieldWord(@Slot(1) Object object, int offset, @Slot(0) Word value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, offset, value.asAddress().toLong());
        }
        TupleAccess.writeWord(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Word value, int bci) {
        resolveAndPutFieldWord(guard, object, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value, int bci) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(bci, object, f.offset(), value.asAddress().toLong());
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
    public static void putstaticWord(Object staticTuple, int offset, @Slot(0) Word value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, staticTuple, offset, value.asAddress().toLong());
        }
        TupleAccess.writeWord(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard, @Slot(0) Word value, int bci) {
        resolveAndPutStaticWord(guard, value, bci);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value, int bci) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(bci, f.holder().staticTuple(), f.offset(), value.asAddress().toLong());
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

    @T1X_TEMPLATE(INVOKEVIRTUAL$word)
    @Slot(-1)
    public static Address invokevirtualWord(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$word$resolved)
    @Slot(-1)
    public static Address invokevirtualWord(VirtualMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$word$instrumented)
    @Slot(-1)
    public static Address invokevirtualWord(VirtualMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$word)
    @Slot(-1)
    public static Address invokeinterfaceWord(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$word$resolved)
    @Slot(-1)
    public static Address invokeinterfaceWord(InterfaceMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$word$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceWord(InterfaceMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$word)
    @Slot(-1)
    public static Address invokespecialWord(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        VirtualMethodActor methodActor = VMAT1XRuntime.resolveSpecialMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.initializeSpecialMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESPECIAL$word$resolved)
    public static void invokespecialWord(VirtualMethodActor methodActor, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKESTATIC$word)
    @Slot(-1)
    public static Address invokestaticWord(ResolutionGuard.InPool guard, int bci) {
        StaticMethodActor methodActor = VMAT1XRuntime.resolveStaticMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
        return VMAT1XRuntime.initializeStaticMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESTATIC$word$init)
    public static void invokestaticWord(StaticMethodActor methodActor, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
    }

    @T1X_TEMPLATE(RETURN)
    @Slot(-1)
    public static void vreturn(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci);
        }
    }

    @T1X_TEMPLATE(RETURN$unlock)
    @Slot(-1)
    public static void vreturnUnlock(Reference object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.noninlineExit(object);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci);
        }
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$void)
    @Slot(-1)
    public static Address invokevirtualVoid(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        VirtualMethodActor methodActor = Snippets.resolveVirtualMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectNonPrivateVirtualMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$void$resolved)
    @Slot(-1)
    public static Address invokevirtualVoid(VirtualMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return ObjectAccess.readHub(receiver).getWord(methodActor.vTableIndex()).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEVIRTUAL$void$instrumented)
    @Slot(-1)
    public static Address invokevirtualVoid(VirtualMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeVirtual(bci, receiver, methodActor);
        }
        return selectVirtualMethod(receiver, methodActor.vTableIndex(), mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$void)
    @Slot(-1)
    public static Address invokeinterfaceVoid(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        final InterfaceMethodActor methodActor = Snippets.resolveInterfaceMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$void$resolved)
    @Slot(-1)
    public static Address invokeinterfaceVoid(InterfaceMethodActor methodActor, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.selectInterfaceMethod(receiver, methodActor).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKEINTERFACE$void$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceVoid(InterfaceMethodActor methodActor, MethodProfile mpo, int mpoIndex, Reference receiver, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeInterface(bci, receiver, methodActor);
        }
        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    @T1X_TEMPLATE(INVOKESPECIAL$void)
    @Slot(-1)
    public static Address invokespecialVoid(ResolutionGuard.InPool guard, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        VirtualMethodActor methodActor = VMAT1XRuntime.resolveSpecialMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
        return VMAT1XRuntime.initializeSpecialMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESPECIAL$void$resolved)
    public static void invokespecialVoid(VirtualMethodActor methodActor, Reference receiver, int bci) {
        nullCheck(receiver.toOrigin());
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeSpecial(bci, receiver, methodActor);
        }
    }

    @T1X_TEMPLATE(INVOKESTATIC$void)
    @Slot(-1)
    public static Address invokestaticVoid(ResolutionGuard.InPool guard, int bci) {
        StaticMethodActor methodActor = VMAT1XRuntime.resolveStaticMethod(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
        return VMAT1XRuntime.initializeStaticMethod(methodActor);
    }

    @T1X_TEMPLATE(INVOKESTATIC$void$init)
    public static void invokestaticVoid(StaticMethodActor methodActor, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInvokeStatic(bci, null, methodActor);
        }
    }

    @T1X_TEMPLATE(LCMP)
    public static int lcmp(@Slot(2) long value1, @Slot(0) long value2, int bci) {
        int result = rawCompare(Bytecodes.LCMP, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 148, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(FCMPL)
    public static int fcmpl(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        int result = rawCompare(Bytecodes.FCMPL, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 149, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(FCMPG)
    public static int fcmpg(@Slot(1) float value1, @Slot(0) float value2, int bci) {
        int result = rawCompare(Bytecodes.FCMPG, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 150, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(DCMPL)
    public static int dcmpl(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        int result = rawCompare(Bytecodes.DCMPL, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 151, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(DCMPG)
    public static int dcmpg(@Slot(2) double value1, @Slot(0) double value2, int bci) {
        int result = rawCompare(Bytecodes.DCMPG, value1, value2);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 152, value1, value2);
        }
        return result;
    }

    @T1X_TEMPLATE(CHECKCAST)
    public static Object checkcast(ResolutionGuard guard, @Slot(0) Object object, int bci) {
        resolveAndCheckcast(guard, object, bci);
        return object;
    }

    @T1X_TEMPLATE(CHECKCAST$resolved)
    public static Object checkcast(ClassActor classActor, @Slot(0) Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeCheckCast(bci, object, classActor);
        }
        Snippets.checkCast(classActor, object);
        return object;
    }

    @NEVER_INLINE
    private static void resolveAndCheckcast(ResolutionGuard guard, final Object object, int bci) {
        ClassActor classActor = Snippets.resolveClass(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeCheckCast(bci, object, classActor);
        }
        Snippets.checkCast(classActor, object);
    }

    @T1X_TEMPLATE(ARRAYLENGTH)
    public static int arraylength(@Slot(0) Object array, int bci) {
        int length = ArrayAccess.readArrayLength(array);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLength(bci, array, length);
        }
        return length;
    }

    @T1X_TEMPLATE(ATHROW)
    public static void athrow(@Slot(0) Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeThrow(bci, object);
        }
        Throw.raise(object);
    }

    @T1X_TEMPLATE(RETHROW_EXCEPTION)
    public static void rethrowException(int bci) {
        Throwable throwable = VmThread.current().loadExceptionForHandler();
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeThrow(bci, throwable);
        }
        Throw.raise(throwable);
    }

    @T1X_TEMPLATE(MONITORENTER)
    public static void monitorenter(@Slot(0) Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorEnter(bci, object);
        }
        Monitor.enter(object);
    }

    @T1X_TEMPLATE(MONITOREXIT)
    public static void monitorexit(@Slot(0) Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.exit(object);
    }

    @T1X_TEMPLATE(INSTANCEOF)
    public static int instanceof_(ResolutionGuard guard, @Slot(0) Object object, int bci) {
        ClassActor classActor = Snippets.resolveClass(guard);
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInstanceOf(bci, object, classActor);
        }
        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));
    }

    @T1X_TEMPLATE(INSTANCEOF$resolved)
    public static int instanceof_(ClassActor classActor, @Slot(0) Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeInstanceOf(bci, object, classActor);
        }
        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));
    }

    @T1X_TEMPLATE(RETURN$registerFinalizer)
    @Slot(-1)
    public static void vreturnRegisterFinalizer(Reference object, int bci) {
        if (ObjectAccess.readClassActor(object).hasFinalizer()) {
            SpecialReferenceManager.registerFinalizee(object);
        }
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeReturn(bci);
        }
    }

    @T1X_TEMPLATE(LOCK)
    public static void lock(Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorEnter(bci, object);
        }
        Monitor.enter(object);
    }

    @T1X_TEMPLATE(UNLOCK)
    public static void unlock(Object object, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeMonitorExit(bci, object);
        }
        Monitor.exit(object);
    }

    @T1X_TEMPLATE(LOAD_EXCEPTION)
    public static Object loadException(int bci) {
        Object exception = VmThread.current().loadExceptionForHandler();
        return exception;
    }

    @T1X_TEMPLATE(POP)
    public static void pop(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 87);
        }
    }

    @T1X_TEMPLATE(POP2)
    public static void pop2(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 88);
        }
    }

    @T1X_TEMPLATE(DUP)
    public static void dup(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 89);
        }
    }

    @T1X_TEMPLATE(DUP_X1)
    public static void dup_x1(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 90);
        }
    }

    @T1X_TEMPLATE(DUP_X2)
    public static void dup_x2(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 91);
        }
    }

    @T1X_TEMPLATE(DUP2)
    public static void dup2(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 92);
        }
    }

    @T1X_TEMPLATE(DUP2_X1)
    public static void dup2_x1(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 93);
        }
    }

    @T1X_TEMPLATE(DUP2_X2)
    public static void dup2_x2(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 94);
        }
    }

    @T1X_TEMPLATE(SWAP)
    public static void swap(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStackAdjust(bci, 95);
        }
    }

    @T1X_TEMPLATE(ACONST_NULL)
    public static void aconst_null(int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, null);
        }
    }

    @T1X_TEMPLATE(ICONST)
    public static void iconst(int constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LCONST)
    public static void lconst(long constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(FCONST)
    public static void fconst(float constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(DCONST)
    public static void dconst(double constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LDC$int)
    public static void ildc(int constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LDC$long)
    public static void lldc(long constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LDC$float)
    public static void fldc(float constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LDC$double)
    public static void dldc(double constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LDC$reference)
    public static void uoldc(ResolutionGuard guard, int bci) {
        ClassActor classActor = Snippets.resolveClass(guard);
        Object constant = classActor.javaClass();
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(LDC$reference$resolved)
    public static void oldc(Object constant, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeConstLoad(bci, constant);
        }
    }

    @T1X_TEMPLATE(ILOAD)
    public static void iload(int index, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(bci, index);
        }
    }

    @T1X_TEMPLATE(LLOAD)
    public static void lload(int index, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(bci, index);
        }
    }

    @T1X_TEMPLATE(FLOAD)
    public static void fload(int index, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(bci, index);
        }
    }

    @T1X_TEMPLATE(DLOAD)
    public static void dload(int index, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(bci, index);
        }
    }

    @T1X_TEMPLATE(ALOAD)
    public static void oload(int index, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeLoad(bci, index);
        }
    }

    @T1X_TEMPLATE(ISTORE)
    public static void istore(int index, int value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(bci, index, value);
        }
    }

    @T1X_TEMPLATE(LSTORE)
    public static void lstore(int index, long value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(bci, index, value);
        }
    }

    @T1X_TEMPLATE(FSTORE)
    public static void fstore(int index, float value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(bci, index, value);
        }
    }

    @T1X_TEMPLATE(DSTORE)
    public static void dstore(int index, double value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(bci, index, value);
        }
    }

    @T1X_TEMPLATE(ASTORE)
    public static void ostore(int index, Object value, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeStore(bci, index, value);
        }
    }

    @T1X_TEMPLATE(IINC)
    public static void iinc(int index, int increment, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeOperation(bci, 132, index, increment);
        }
    }

    @T1X_TEMPLATE(IFEQ)
    public static void ifeq(int value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 153, value1, 0, targetBci);
        }
    }

    @T1X_TEMPLATE(IFNE)
    public static void ifne(int value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 154, value1, 0, targetBci);
        }
    }

    @T1X_TEMPLATE(IFLT)
    public static void iflt(int value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 155, value1, 0, targetBci);
        }
    }

    @T1X_TEMPLATE(IFGE)
    public static void ifge(int value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 156, value1, 0, targetBci);
        }
    }

    @T1X_TEMPLATE(IFGT)
    public static void ifgt(int value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 157, value1, 0, targetBci);
        }
    }

    @T1X_TEMPLATE(IFLE)
    public static void ifle(int value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 158, value1, 0, targetBci);
        }
    }

    @T1X_TEMPLATE(IFNULL)
    public static void ifnull(Object value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 198, value1, null, targetBci);
        }
    }

    @T1X_TEMPLATE(IFNONNULL)
    public static void ifnonnull(Object value1, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 199, value1, null, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ICMPEQ)
    public static void if_icmpeq(int value1, int value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 159, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ICMPNE)
    public static void if_icmpne(int value1, int value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 160, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ICMPLT)
    public static void if_icmplt(int value1, int value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 161, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ICMPGE)
    public static void if_icmpge(int value1, int value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 162, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ICMPGT)
    public static void if_icmpgt(int value1, int value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 163, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ICMPLE)
    public static void if_icmple(int value1, int value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 164, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ACMPEQ)
    public static void if_acmpeq(Object value1, Object value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 165, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(IF_ACMPNE)
    public static void if_acmpne(Object value1, Object value2, int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeIf(bci, 166, value1, value2, targetBci);
        }
    }

    @T1X_TEMPLATE(GOTO)
    public static void goto_s(int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGoto(bci, targetBci);
        }
    }

    @T1X_TEMPLATE(GOTO_W)
    public static void goto_w(int targetBci, int bci) {
        if (Intrinsics.readLatchBit(VMAJavaRunScheme.VM_ADVISING.offset, 0)) {
            VMAStaticBytecodeAdvice.adviseBeforeGoto(bci, targetBci);
        }
    }

// END GENERATED CODE

}
