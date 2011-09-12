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
package com.oracle.max.vm.ext.t1x;

import static com.oracle.max.vm.ext.t1x.T1XRuntime.*;
import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytecodes.Infopoints;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The Java source for the templates used by T1X.
 *
 * The templates are almost all automatically generated as many bytecodes fall in groups that share a very similar implementation.
 * Auto-generation also allows for easy and optional customization, in particular, tracking of bytecode execution.
 * The automatically generated code is created by running {@link T1XTemplateGenerator#main} and is inserted (manually) at the end of the class.
 */
public class T1XTemplateSource {

    @INTRINSIC(Bytecodes.LCMP)
    public static native int lcmp(long l, long r);
    @INTRINSIC(Bytecodes.FCMPG)
    public static native int fcmpg(float l, float r);
    @INTRINSIC(Bytecodes.FCMPL)
    public static native int fcmpl(float l, float r);
    @INTRINSIC(Bytecodes.DCMPG)
    public static native int dcmpg(double l, double r);
    @INTRINSIC(Bytecodes.DCMPL)
    public static native int dcmpl(double l, double r);

    @T1X_TEMPLATE(HERE)
    public static Pointer here() {
        return Pointer.fromLong(Infopoints.here());
    }

    @T1X_TEMPLATE(LOAD_EXCEPTION)
    public static Object loadException() {
        return T1XRuntime.loadException();
    }

    @T1X_TEMPLATE(RETHROW_EXCEPTION)
    public static void rethrowException() {
        T1XRuntime.rethrowException();
    }

    @T1X_TEMPLATE(PROFILE_NONSTATIC_METHOD_ENTRY)
    public static void profileNonstaticMethodEntry(MethodProfile mpo, Object rcvr) {
        // entrypoint counters count down to zero ("overflow")
        MethodInstrumentation.recordEntrypoint(mpo, rcvr);
    }

    @T1X_TEMPLATE(PROFILE_STATIC_METHOD_ENTRY)
    public static void profileStaticMethodEntry(MethodProfile mpo) {
        // entrypoint counters count down to zero ("overflow")
        MethodInstrumentation.recordEntrypoint(mpo, null);
    }

    @T1X_TEMPLATE(PROFILE_BACKWARD_BRANCH)
    public static void profileBackwardBranch(MethodProfile mpo) {
        // entrypoint counters count down to zero ("overflow")
        // Currently, there is no reason to use a separate counter for backward branches.
        MethodInstrumentation.recordBackwardBranch(mpo);
    }

    @INTRINSIC(Bytecodes.UNSAFE_CAST)
    public static native Word toWord(Object object);

    @INLINE
    public static void nullCheck(Pointer receiver) {
        receiver.readWord(0);
    }

    @INLINE
    public static Address selectVirtualMethod(Object receiver, int vTableIndex, MethodProfile mpo, int mpoIndex) {
        Hub hub = ObjectAccess.readHub(receiver);
        Address entryPoint = hub.getWord(vTableIndex).asAddress();
        MethodInstrumentation.recordType(mpo, hub, mpoIndex, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES);
        return entryPoint;
    }

    @T1X_TEMPLATE(LSB)
    public static int lsb(@Slot(0) Word value) {
        return value.leastSignificantBitSet();
    }

    @T1X_TEMPLATE(MSB)
    public static int msb(@Slot(0) Word value) {
        return value.mostSignificantBitSet();
    }

    @T1X_TEMPLATE(MEMBAR_LOAD_LOAD)
    public static void membar_load_load() {
        loadLoad();
    }

    @T1X_TEMPLATE(MEMBAR_LOAD_STORE)
    public static void membar_load_store() {
        loadStore();
    }

    @T1X_TEMPLATE(MEMBAR_STORE_STORE)
    public static void membar_store_store() {
        loadStore();
    }

    @T1X_TEMPLATE(MEMBAR_STORE_LOAD)
    public static void membar_store_load() {
        storeLoad();
    }

    @T1X_TEMPLATE(PAUSE)
    public static void pause() {
        Intrinsics.pause();
    }

    @T1X_TEMPLATE(LDC$reference)
    public static Object ldc(ResolutionGuard guard) {
        ClassActor classActor = Snippets.resolveClass(guard);
        return T1XRuntime.getClassMirror(classActor);
    }

    /**
     * Helper template that pops the dimensions of a multianewarray off the stack
     * into an int[] array. The array is returned in the (platform-dependent) register
     * used for an object return value (hence the {@code @Slot(-1)} annotation).
     */
    @Slot(-1)
    @T1X_TEMPLATE(CREATE_MULTIANEWARRAY_DIMENSIONS)
    public static Object createMultianewarrayDimensions(Pointer sp, int n) {
        return T1XRuntime.createMultianewarrayDimensions(sp, n);
    }

    @T1X_TEMPLATE(LOCK)
    public static void lock(Object object) {
        T1XRuntime.monitorenter(object);
    }

    @T1X_TEMPLATE(UNLOCK)
    public static void unlock(Object object) {
        T1XRuntime.monitorexit(object);
    }

// START GENERATED CODE
    @T1X_TEMPLATE(GETFIELD$boolean$resolved)
    public static int getfieldBoolean(@Slot(0) Object object, int offset) {
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
        boolean result = TupleAccess.readBoolean(staticTuple, offset);
        return UnsafeCast.asByte(result);
    }

    @T1X_TEMPLATE(PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(@Slot(1) Object object, int offset, @Slot(0) int value) {
        TupleAccess.writeBoolean(object, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldBoolean(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeBoolean(staticTuple, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticBoolean(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        byte result = TupleAccess.readByte(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$byte$resolved)
    public static void putfieldByte(@Slot(1) Object object, int offset, @Slot(0) int value) {
        TupleAccess.writeByte(object, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldByte(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeByte(staticTuple, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticByte(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return (byte) value;
    }

    @T1X_TEMPLATE(BALOAD)
    public static int baload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        byte result = ArrayAccess.getByte(array, index);
        return result;
    }

    @T1X_TEMPLATE(BASTORE)
    public static void bastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setByte(array, index, (byte) value);
    }

    @T1X_TEMPLATE(PGET_BYTE)
    public static int pget_byte(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getByte(disp, index);
    }

    @T1X_TEMPLATE(PSET_BYTE)
    public static void pset_byte(@Slot(3) Pointer pointer, @Slot(2) int disp, @Slot(1) int index, @Slot(0) int value) {
        pointer.setByte(disp, index, (byte) value);
    }

    @T1X_TEMPLATE(PREAD_BYTE)
    public static int pread_byte(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readByte(offset);
    }

    @T1X_TEMPLATE(PWRITE_BYTE)
    public static void pwrite_byte(@Slot(2) Pointer pointer, @Slot(1) Offset offset, @Slot(0) int value) {
        pointer.writeByte(offset, (byte) value);
    }

    @T1X_TEMPLATE(PREAD_BYTE_I)
    public static int pread_byte_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readByte(offset);
    }

    @T1X_TEMPLATE(PWRITE_BYTE_I)
    public static void pwrite_byte_i(@Slot(2) Pointer pointer, @Slot(1) int offset, @Slot(0) int value) {
        pointer.writeByte(offset, (byte) value);
    }

    @T1X_TEMPLATE(GETFIELD$char$resolved)
    public static int getfieldChar(@Slot(0) Object object, int offset) {
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
        char result = TupleAccess.readChar(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$char$resolved)
    public static void putfieldChar(@Slot(1) Object object, int offset, @Slot(0) int value) {
        TupleAccess.writeChar(object, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldChar(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeChar(staticTuple, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticChar(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return (char) value;
    }

    @T1X_TEMPLATE(CALOAD)
    public static int caload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        char result = ArrayAccess.getChar(array, index);
        return result;
    }

    @T1X_TEMPLATE(CASTORE)
    public static void castore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setChar(array, index, (char) value);
    }

    @T1X_TEMPLATE(PGET_CHAR)
    public static int pget_char(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getChar(disp, index);
    }

    @T1X_TEMPLATE(PREAD_CHAR)
    public static int pread_char(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readChar(offset);
    }

    @T1X_TEMPLATE(PREAD_CHAR_I)
    public static int pread_char_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readChar(offset);
    }

    @T1X_TEMPLATE(GETFIELD$short$resolved)
    public static int getfieldShort(@Slot(0) Object object, int offset) {
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
        short result = TupleAccess.readShort(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$short$resolved)
    public static void putfieldShort(@Slot(1) Object object, int offset, @Slot(0) int value) {
        TupleAccess.writeShort(object, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldShort(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeShort(staticTuple, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticShort(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return (short) value;
    }

    @T1X_TEMPLATE(SALOAD)
    public static int saload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        short result = ArrayAccess.getShort(array, index);
        return result;
    }

    @T1X_TEMPLATE(SASTORE)
    public static void sastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setShort(array, index, (short) value);
    }

    @T1X_TEMPLATE(PGET_SHORT)
    public static int pget_short(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getShort(disp, index);
    }

    @T1X_TEMPLATE(PSET_SHORT)
    public static void pset_short(@Slot(3) Pointer pointer, @Slot(2) int disp, @Slot(1) int index, @Slot(0) int value) {
        pointer.setShort(disp, index, (short) value);
    }

    @T1X_TEMPLATE(PREAD_SHORT)
    public static int pread_short(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readShort(offset);
    }

    @T1X_TEMPLATE(PWRITE_SHORT)
    public static void pwrite_short(@Slot(2) Pointer pointer, @Slot(1) Offset offset, @Slot(0) int value) {
        pointer.writeShort(offset, (short) value);
    }

    @T1X_TEMPLATE(PREAD_SHORT_I)
    public static int pread_short_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readShort(offset);
    }

    @T1X_TEMPLATE(PWRITE_SHORT_I)
    public static void pwrite_short_i(@Slot(2) Pointer pointer, @Slot(1) int offset, @Slot(0) int value) {
        pointer.writeShort(offset, (short) value);
    }

    @T1X_TEMPLATE(GETFIELD$int$resolved)
    public static int getfieldInt(@Slot(0) Object object, int offset) {
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
        int result = TupleAccess.readInt(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$int$resolved)
    public static void putfieldInt(@Slot(1) Object object, int offset, @Slot(0) int value) {
        TupleAccess.writeInt(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldInt(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticInt(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return (int) value;
    }

    @T1X_TEMPLATE(F2I)
    public static int f2i(@Slot(0) float value) {
        return T1XRuntime.f2i(value);
    }

    @T1X_TEMPLATE(D2I)
    public static int d2i(@Slot(0) double value) {
        return T1XRuntime.d2i(value);
    }

    @T1X_TEMPLATE(IADD)
    public static int iadd(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 + value2;
    }

    @T1X_TEMPLATE(ISUB)
    public static int isub(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 - value2;
    }

    @T1X_TEMPLATE(IMUL)
    public static int imul(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 * value2;
    }

    @T1X_TEMPLATE(IDIV)
    public static int idiv(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 / value2;
    }

    @T1X_TEMPLATE(IREM)
    public static int irem(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 % value2;
    }

    @T1X_TEMPLATE(INEG)
    public static int ineg(@Slot(0) int value, int zero) {
        return zero - value;
    }

    @T1X_TEMPLATE(IOR)
    public static int ior(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 | value2;
    }

    @T1X_TEMPLATE(IAND)
    public static int iand(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 & value2;
    }

    @T1X_TEMPLATE(IXOR)
    public static int ixor(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 ^ value2;
    }

    @T1X_TEMPLATE(ISHL)
    public static int ishl(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 << value2;
    }

    @T1X_TEMPLATE(ISHR)
    public static int ishr(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 >> value2;
    }

    @T1X_TEMPLATE(IUSHR)
    public static int iushr(@Slot(1) int value1, @Slot(0) int value2) {
        return value1 >>> value2;
    }

    @T1X_TEMPLATE(IRETURN)
    public static int ireturn(@Slot(0) int value) {
        return value;
    }

    @T1X_TEMPLATE(IRETURN$unlock)
    public static int ireturnUnlock(Reference object, @Slot(0) int value) {
        Monitor.noninlineExit(object);
        return value;
    }

    @T1X_TEMPLATE(IALOAD)
    public static int iaload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        int result = ArrayAccess.getInt(array, index);
        return result;
    }

    @T1X_TEMPLATE(IASTORE)
    public static void iastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) int value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setInt(array, index, value);
    }

    @T1X_TEMPLATE(PGET_INT)
    public static int pget_int(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getInt(disp, index);
    }

    @T1X_TEMPLATE(PSET_INT)
    public static void pset_int(@Slot(3) Pointer pointer, @Slot(2) int disp, @Slot(1) int index, @Slot(0) int value) {
        pointer.setInt(disp, index, value);
    }

    @T1X_TEMPLATE(PREAD_INT)
    public static int pread_int(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readInt(offset);
    }

    @T1X_TEMPLATE(PWRITE_INT)
    public static void pwrite_int(@Slot(2) Pointer pointer, @Slot(1) Offset offset, @Slot(0) int value) {
        pointer.writeInt(offset, value);
    }

    @T1X_TEMPLATE(PREAD_INT_I)
    public static int pread_int_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readInt(offset);
    }

    @T1X_TEMPLATE(PWRITE_INT_I)
    public static void pwrite_int_i(@Slot(2) Pointer pointer, @Slot(1) int offset, @Slot(0) int value) {
        pointer.writeInt(offset, value);
    }

    @T1X_TEMPLATE(PCMPSWP_INT)
    public static int pcmpswp_int(@Slot(3) Pointer ptr, @Slot(2) Offset off, @Slot(1) int expectedValue, @Slot(0) int newValue) {
        return ptr.compareAndSwapInt(off, expectedValue, newValue);
    }

    @T1X_TEMPLATE(PCMPSWP_INT_I)
    public static int pcmpswp_int_i(@Slot(3) Pointer ptr, @Slot(2) int off, @Slot(1) int expectedValue, @Slot(0) int newValue) {
        return ptr.compareAndSwapInt(off, expectedValue, newValue);
    }

    @T1X_TEMPLATE(GETFIELD$float$resolved)
    public static float getfieldFloat(@Slot(0) Object object, int offset) {
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
        float result = TupleAccess.readFloat(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$float$resolved)
    public static void putfieldFloat(@Slot(1) Object object, int offset, @Slot(0) float value) {
        TupleAccess.writeFloat(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) float value) {
        resolveAndPutFieldFloat(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard, @Slot(0) float value) {
        resolveAndPutStaticFloat(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return value;
    }

    @T1X_TEMPLATE(L2F)
    public static float l2f(@Slot(0) long value) {
        return value;
    }

    @T1X_TEMPLATE(D2F)
    public static float d2f(@Slot(0) double value) {
        return (float) value;
    }

    @T1X_TEMPLATE(FADD)
    public static float fadd(@Slot(1) float value1, @Slot(0) float value2) {
        return value1 + value2;
    }

    @T1X_TEMPLATE(FSUB)
    public static float fsub(@Slot(1) float value1, @Slot(0) float value2) {
        return value1 - value2;
    }

    @T1X_TEMPLATE(FMUL)
    public static float fmul(@Slot(1) float value1, @Slot(0) float value2) {
        return value1 * value2;
    }

    @T1X_TEMPLATE(FDIV)
    public static float fdiv(@Slot(1) float value1, @Slot(0) float value2) {
        return value1 / value2;
    }

    @T1X_TEMPLATE(FREM)
    public static float frem(@Slot(1) float value1, @Slot(0) float value2) {
        return value1 % value2;
    }

    @T1X_TEMPLATE(FNEG)
    public static float fneg(@Slot(0) float value, float zero) {
        return zero - value;
    }

    @T1X_TEMPLATE(FCMPG)
    public static int fcmpgOp(@Slot(1) float value1, @Slot(0) float value2) {
        int result = fcmpg(value1, value2);
        return result;
    }

    @T1X_TEMPLATE(FCMPL)
    public static int fcmplOp(@Slot(1) float value1, @Slot(0) float value2) {
        int result = fcmpl(value1, value2);
        return result;
    }

    @T1X_TEMPLATE(FRETURN)
    public static float freturn(@Slot(0) float value) {
        return value;
    }

    @T1X_TEMPLATE(FRETURN$unlock)
    public static float freturnUnlock(Reference object, @Slot(0) float value) {
        Monitor.noninlineExit(object);
        return value;
    }

    @T1X_TEMPLATE(FALOAD)
    public static float faload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        float result = ArrayAccess.getFloat(array, index);
        return result;
    }

    @T1X_TEMPLATE(FASTORE)
    public static void fastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) float value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setFloat(array, index, value);
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$float)
    @Slot(-1)
    public static Address invokevirtualFloat(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectVirtualMethod(receiver, guard);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$float$resolved)
    @Slot(-1)
    public static Address invokevirtualFloat(int vTableIndex, Reference receiver) {
        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$float$instrumented)
    @Slot(-1)
    public static Address invokevirtualFloat(int vTableIndex, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEINTERFACE instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$float)
    @Slot(-1)
    public static Address invokeinterfaceFloat(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectInterfaceMethod(guard, receiver);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$float$resolved)
    @Slot(-1)
    public static Address invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$float$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves a method referenced by an INVOKESPECIAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESPECIAL$float)
    @Slot(-1)
    public static Address invokespecialFloat(ResolutionGuard.InPool guard, Reference receiver) {
        nullCheck(receiver.toOrigin());
        return resolveSpecialMethod(guard);
    }

    /**
     * Resolves a method referenced by an INVOKESTATIC instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESTATIC$float)
    @Slot(-1)
    public static Address invokestaticFloat(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @T1X_TEMPLATE(PGET_FLOAT)
    public static float pget_float(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getFloat(disp, index);
    }

    @T1X_TEMPLATE(PSET_FLOAT)
    public static void pset_float(@Slot(3) Pointer pointer, @Slot(2) int disp, @Slot(1) int index, @Slot(0) float value) {
        pointer.setFloat(disp, index, value);
    }

    @T1X_TEMPLATE(PREAD_FLOAT)
    public static float pread_float(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readFloat(offset);
    }

    @T1X_TEMPLATE(PWRITE_FLOAT)
    public static void pwrite_float(@Slot(2) Pointer pointer, @Slot(1) Offset offset, @Slot(0) float value) {
        pointer.writeFloat(offset, value);
    }

    @T1X_TEMPLATE(PREAD_FLOAT_I)
    public static float pread_float_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readFloat(offset);
    }

    @T1X_TEMPLATE(PWRITE_FLOAT_I)
    public static void pwrite_float_i(@Slot(2) Pointer pointer, @Slot(1) int offset, @Slot(0) float value) {
        pointer.writeFloat(offset, value);
    }

    @T1X_TEMPLATE(GETFIELD$long$resolved)
    public static long getfieldLong(@Slot(0) Object object, int offset) {
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
        long result = TupleAccess.readLong(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$long$resolved)
    public static void putfieldLong(@Slot(2) Object object, int offset, @Slot(0) long value) {
        TupleAccess.writeLong(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) long value) {
        resolveAndPutFieldLong(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard, @Slot(0) long value) {
        resolveAndPutStaticLong(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return value;
    }

    @T1X_TEMPLATE(F2L)
    public static long f2l(@Slot(0) float value) {
        return T1XRuntime.f2l(value);
    }

    @T1X_TEMPLATE(D2L)
    public static long d2l(@Slot(0) double value) {
        return T1XRuntime.d2l(value);
    }

    @T1X_TEMPLATE(LADD)
    public static long ladd(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 + value2;
    }

    @T1X_TEMPLATE(LSUB)
    public static long lsub(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 - value2;
    }

    @T1X_TEMPLATE(LMUL)
    public static long lmul(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 * value2;
    }

    @T1X_TEMPLATE(LDIV)
    public static long ldiv(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 / value2;
    }

    @T1X_TEMPLATE(LREM)
    public static long lrem(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 % value2;
    }

    @T1X_TEMPLATE(LNEG)
    public static long lneg(@Slot(0) long value, long zero) {
        return zero - value;
    }

    @T1X_TEMPLATE(LOR)
    public static long lor(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 | value2;
    }

    @T1X_TEMPLATE(LAND)
    public static long land(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 & value2;
    }

    @T1X_TEMPLATE(LXOR)
    public static long lxor(@Slot(2) long value1, @Slot(0) long value2) {
        return value1 ^ value2;
    }

    @T1X_TEMPLATE(LSHL)
    public static long lshl(@Slot(1) long value1, @Slot(0) int value2) {
        return value1 << value2;
    }

    @T1X_TEMPLATE(LSHR)
    public static long lshr(@Slot(1) long value1, @Slot(0) int value2) {
        return value1 >> value2;
    }

    @T1X_TEMPLATE(LUSHR)
    public static long lushr(@Slot(1) long value1, @Slot(0) int value2) {
        return value1 >>> value2;
    }

    @T1X_TEMPLATE(LCMP)
    public static int lcmpOp(@Slot(2) long value1, @Slot(0) long value2) {
        int result = lcmp(value1, value2);
        return result;
    }

    @T1X_TEMPLATE(LRETURN)
    public static long lreturn(@Slot(0) long value) {
        return value;
    }

    @T1X_TEMPLATE(LRETURN$unlock)
    public static long lreturnUnlock(Reference object, @Slot(0) long value) {
        Monitor.noninlineExit(object);
        return value;
    }

    @T1X_TEMPLATE(LALOAD)
    public static long laload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        long result = ArrayAccess.getLong(array, index);
        return result;
    }

    @T1X_TEMPLATE(LASTORE)
    public static void lastore(@Slot(3) Object array, @Slot(2) int index, @Slot(0) long value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setLong(array, index, value);
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$long)
    @Slot(-1)
    public static Address invokevirtualLong(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectVirtualMethod(receiver, guard);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$long$resolved)
    @Slot(-1)
    public static Address invokevirtualLong(int vTableIndex, Reference receiver) {
        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$long$instrumented)
    @Slot(-1)
    public static Address invokevirtualLong(int vTableIndex, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEINTERFACE instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$long)
    @Slot(-1)
    public static Address invokeinterfaceLong(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectInterfaceMethod(guard, receiver);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$long$resolved)
    @Slot(-1)
    public static Address invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$long$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves a method referenced by an INVOKESPECIAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESPECIAL$long)
    @Slot(-1)
    public static Address invokespecialLong(ResolutionGuard.InPool guard, Reference receiver) {
        nullCheck(receiver.toOrigin());
        return resolveSpecialMethod(guard);
    }

    /**
     * Resolves a method referenced by an INVOKESTATIC instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESTATIC$long)
    @Slot(-1)
    public static Address invokestaticLong(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @T1X_TEMPLATE(PGET_LONG)
    public static long pget_long(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getLong(disp, index);
    }

    @T1X_TEMPLATE(PSET_LONG)
    public static void pset_long(@Slot(4) Pointer pointer, @Slot(3) int disp, @Slot(2) int index, @Slot(0) long value) {
        pointer.setLong(disp, index, value);
    }

    @T1X_TEMPLATE(PREAD_LONG)
    public static long pread_long(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readLong(offset);
    }

    @T1X_TEMPLATE(PWRITE_LONG)
    public static void pwrite_long(@Slot(3) Pointer pointer, @Slot(2) Offset offset, @Slot(0) long value) {
        pointer.writeLong(offset, value);
    }

    @T1X_TEMPLATE(PREAD_LONG_I)
    public static long pread_long_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readLong(offset);
    }

    @T1X_TEMPLATE(PWRITE_LONG_I)
    public static void pwrite_long_i(@Slot(3) Pointer pointer, @Slot(2) int offset, @Slot(0) long value) {
        pointer.writeLong(offset, value);
    }

    @T1X_TEMPLATE(GETFIELD$double$resolved)
    public static double getfieldDouble(@Slot(0) Object object, int offset) {
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
        double result = TupleAccess.readDouble(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$double$resolved)
    public static void putfieldDouble(@Slot(2) Object object, int offset, @Slot(0) double value) {
        TupleAccess.writeDouble(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) double value) {
        resolveAndPutFieldDouble(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard, @Slot(0) double value) {
        resolveAndPutStaticDouble(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
        return value;
    }

    @T1X_TEMPLATE(L2D)
    public static double l2d(@Slot(0) long value) {
        return value;
    }

    @T1X_TEMPLATE(F2D)
    public static double f2d(@Slot(0) float value) {
        return value;
    }

    @T1X_TEMPLATE(DADD)
    public static double dadd(@Slot(2) double value1, @Slot(0) double value2) {
        return value1 + value2;
    }

    @T1X_TEMPLATE(DSUB)
    public static double dsub(@Slot(2) double value1, @Slot(0) double value2) {
        return value1 - value2;
    }

    @T1X_TEMPLATE(DMUL)
    public static double dmul(@Slot(2) double value1, @Slot(0) double value2) {
        return value1 * value2;
    }

    @T1X_TEMPLATE(DDIV)
    public static double ddiv(@Slot(2) double value1, @Slot(0) double value2) {
        return value1 / value2;
    }

    @T1X_TEMPLATE(DREM)
    public static double drem(@Slot(2) double value1, @Slot(0) double value2) {
        return value1 % value2;
    }

    @T1X_TEMPLATE(DNEG)
    public static double dneg(@Slot(0) double value, double zero) {
        return zero - value;
    }

    @T1X_TEMPLATE(DCMPG)
    public static int dcmpgOp(@Slot(2) double value1, @Slot(0) double value2) {
        int result = dcmpg(value1, value2);
        return result;
    }

    @T1X_TEMPLATE(DCMPL)
    public static int dcmplOp(@Slot(2) double value1, @Slot(0) double value2) {
        int result = dcmpl(value1, value2);
        return result;
    }

    @T1X_TEMPLATE(DRETURN)
    public static double dreturn(@Slot(0) double value) {
        return value;
    }

    @T1X_TEMPLATE(DRETURN$unlock)
    public static double dreturnUnlock(Reference object, @Slot(0) double value) {
        Monitor.noninlineExit(object);
        return value;
    }

    @T1X_TEMPLATE(DALOAD)
    public static double daload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        double result = ArrayAccess.getDouble(array, index);
        return result;
    }

    @T1X_TEMPLATE(DASTORE)
    public static void dastore(@Slot(3) Object array, @Slot(2) int index, @Slot(0) double value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setDouble(array, index, value);
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$double)
    @Slot(-1)
    public static Address invokevirtualDouble(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectVirtualMethod(receiver, guard);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$double$resolved)
    @Slot(-1)
    public static Address invokevirtualDouble(int vTableIndex, Reference receiver) {
        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$double$instrumented)
    @Slot(-1)
    public static Address invokevirtualDouble(int vTableIndex, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEINTERFACE instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$double)
    @Slot(-1)
    public static Address invokeinterfaceDouble(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectInterfaceMethod(guard, receiver);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$double$resolved)
    @Slot(-1)
    public static Address invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$double$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves a method referenced by an INVOKESPECIAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESPECIAL$double)
    @Slot(-1)
    public static Address invokespecialDouble(ResolutionGuard.InPool guard, Reference receiver) {
        nullCheck(receiver.toOrigin());
        return resolveSpecialMethod(guard);
    }

    /**
     * Resolves a method referenced by an INVOKESTATIC instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESTATIC$double)
    @Slot(-1)
    public static Address invokestaticDouble(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @T1X_TEMPLATE(PGET_DOUBLE)
    public static double pget_double(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getDouble(disp, index);
    }

    @T1X_TEMPLATE(PSET_DOUBLE)
    public static void pset_double(@Slot(4) Pointer pointer, @Slot(3) int disp, @Slot(2) int index, @Slot(0) double value) {
        pointer.setDouble(disp, index, value);
    }

    @T1X_TEMPLATE(PREAD_DOUBLE)
    public static double pread_double(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readDouble(offset);
    }

    @T1X_TEMPLATE(PWRITE_DOUBLE)
    public static void pwrite_double(@Slot(3) Pointer pointer, @Slot(2) Offset offset, @Slot(0) double value) {
        pointer.writeDouble(offset, value);
    }

    @T1X_TEMPLATE(PREAD_DOUBLE_I)
    public static double pread_double_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readDouble(offset);
    }

    @T1X_TEMPLATE(PWRITE_DOUBLE_I)
    public static void pwrite_double_i(@Slot(3) Pointer pointer, @Slot(2) int offset, @Slot(0) double value) {
        pointer.writeDouble(offset, value);
    }

    @T1X_TEMPLATE(GETFIELD$reference$resolved)
    public static Reference getfieldObject(@Slot(0) Object object, int offset) {
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
        Object result = TupleAccess.readObject(staticTuple, offset);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(PUTFIELD$reference$resolved)
    public static void putfieldReference(@Slot(1) Object object, int offset, @Slot(0) Reference value) {
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Reference value) {
        resolveAndPutFieldReference(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Reference value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard, @Slot(0) Reference value) {
        resolveAndPutStaticReference(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Reference value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
    public static Reference areturn(@Slot(0) Reference value) {
        return value;
    }

    @T1X_TEMPLATE(ARETURN$unlock)
    public static Reference areturnUnlock(Reference object, @Slot(0) Reference value) {
        Monitor.noninlineExit(object);
        return value;
    }

    @T1X_TEMPLATE(AALOAD)
    public static Reference aaload(@Slot(1) Object array, @Slot(0) int index) {
        ArrayAccess.checkIndex(array, index);
        Object result = ArrayAccess.getObject(array, index);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(AASTORE)
    public static void aastore(@Slot(2) Object array, @Slot(1) int index, @Slot(0) Reference value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$reference)
    @Slot(-1)
    public static Address invokevirtualObject(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectVirtualMethod(receiver, guard);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$reference$resolved)
    @Slot(-1)
    public static Address invokevirtualObject(int vTableIndex, Reference receiver) {
        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$reference$instrumented)
    @Slot(-1)
    public static Address invokevirtualObject(int vTableIndex, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEINTERFACE instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$reference)
    @Slot(-1)
    public static Address invokeinterfaceObject(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectInterfaceMethod(guard, receiver);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$reference$resolved)
    @Slot(-1)
    public static Address invokeinterfaceObject(InterfaceMethodActor interfaceMethodActor, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$reference$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceObject(InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves a method referenced by an INVOKESPECIAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESPECIAL$reference)
    @Slot(-1)
    public static Address invokespecialObject(ResolutionGuard.InPool guard, Reference receiver) {
        nullCheck(receiver.toOrigin());
        return resolveSpecialMethod(guard);
    }

    /**
     * Resolves a method referenced by an INVOKESTATIC instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESTATIC$reference)
    @Slot(-1)
    public static Address invokestaticObject(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @T1X_TEMPLATE(PGET_REFERENCE)
    public static Reference pget_object(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getReference(disp, index);
    }

    @T1X_TEMPLATE(PSET_REFERENCE)
    public static void pset_object(@Slot(3) Pointer pointer, @Slot(2) int disp, @Slot(1) int index, @Slot(0) Reference value) {
        pointer.setReference(disp, index, value);
    }

    @T1X_TEMPLATE(PREAD_REFERENCE)
    public static Object pread_object(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readReference(offset);
    }

    @T1X_TEMPLATE(PWRITE_REFERENCE)
    public static void pwrite_object(@Slot(2) Pointer pointer, @Slot(1) Offset offset, @Slot(0) Reference value) {
        pointer.writeReference(offset, value);
    }

    @T1X_TEMPLATE(PREAD_REFERENCE_I)
    public static Object pread_object_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readReference(offset);
    }

    @T1X_TEMPLATE(PWRITE_REFERENCE_I)
    public static void pwrite_object_i(@Slot(2) Pointer pointer, @Slot(1) int offset, @Slot(0) Reference value) {
        pointer.writeReference(offset, value);
    }

    @T1X_TEMPLATE(PCMPSWP_REFERENCE)
    public static Reference pcmpswp_reference(@Slot(3) Pointer ptr, @Slot(2) Offset off, @Slot(1) Reference expectedValue, @Slot(0) Reference newValue) {
        return ptr.compareAndSwapReference(off, expectedValue, newValue);
    }

    @T1X_TEMPLATE(PCMPSWP_REFERENCE_I)
    public static Reference pcmpswp_reference_i(@Slot(3) Pointer ptr, @Slot(2) int off, @Slot(1) Reference expectedValue, @Slot(0) Reference newValue) {
        return ptr.compareAndSwapReference(off, expectedValue, newValue);
    }

    @T1X_TEMPLATE(GETFIELD$word$resolved)
    public static Word getfieldWord(@Slot(0) Object object, int offset) {
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
        Word result = TupleAccess.readWord(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTFIELD$word$resolved)
    public static void putfieldWord(@Slot(1) Object object, int offset, @Slot(0) Word value) {
        TupleAccess.writeWord(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Word value) {
        resolveAndPutFieldWord(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
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
        TupleAccess.writeWord(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard, @Slot(0) Word value) {
        resolveAndPutStaticWord(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
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
    public static Word wreturn(@Slot(0) Word value) {
        return value;
    }

    @T1X_TEMPLATE(WRETURN$unlock)
    public static Word wreturnUnlock(Reference object, @Slot(0) Word value) {
        Monitor.noninlineExit(object);
        return value;
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$word)
    @Slot(-1)
    public static Address invokevirtualWord(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectVirtualMethod(receiver, guard);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$word$resolved)
    @Slot(-1)
    public static Address invokevirtualWord(int vTableIndex, Reference receiver) {
        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$word$instrumented)
    @Slot(-1)
    public static Address invokevirtualWord(int vTableIndex, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEINTERFACE instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$word)
    @Slot(-1)
    public static Address invokeinterfaceWord(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectInterfaceMethod(guard, receiver);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$word$resolved)
    @Slot(-1)
    public static Address invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$word$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves a method referenced by an INVOKESPECIAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESPECIAL$word)
    @Slot(-1)
    public static Address invokespecialWord(ResolutionGuard.InPool guard, Reference receiver) {
        nullCheck(receiver.toOrigin());
        return resolveSpecialMethod(guard);
    }

    /**
     * Resolves a method referenced by an INVOKESTATIC instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESTATIC$word)
    @Slot(-1)
    public static Address invokestaticWord(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @T1X_TEMPLATE(PGET_WORD)
    public static Word pget_word(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {
        return pointer.getWord(disp, index);
    }

    @T1X_TEMPLATE(PSET_WORD)
    public static void pset_word(@Slot(3) Pointer pointer, @Slot(2) int disp, @Slot(1) int index, @Slot(0) Word value) {
        pointer.setWord(disp, index, value);
    }

    @T1X_TEMPLATE(PREAD_WORD)
    public static Word pread_word(@Slot(2) Pointer pointer, @Slot(1) Offset offset) {
        return pointer.readWord(offset);
    }

    @T1X_TEMPLATE(PWRITE_WORD)
    public static void pwrite_word(@Slot(2) Pointer pointer, @Slot(1) Offset offset, @Slot(0) Word value) {
        pointer.writeWord(offset, value);
    }

    @T1X_TEMPLATE(PREAD_WORD_I)
    public static Word pread_word_i(@Slot(2) Pointer pointer, @Slot(1) int offset) {
        return pointer.readWord(offset);
    }

    @T1X_TEMPLATE(PWRITE_WORD_I)
    public static void pwrite_word_i(@Slot(2) Pointer pointer, @Slot(1) int offset, @Slot(0) Word value) {
        pointer.writeWord(offset, value);
    }

    @T1X_TEMPLATE(PCMPSWP_WORD)
    public static Word pcmpswp_word(@Slot(3) Pointer ptr, @Slot(2) Offset off, @Slot(1) Word expectedValue, @Slot(0) Word newValue) {
        return ptr.compareAndSwapWord(off, expectedValue, newValue);
    }

    @T1X_TEMPLATE(PCMPSWP_WORD_I)
    public static Word pcmpswp_word_i(@Slot(3) Pointer ptr, @Slot(2) int off, @Slot(1) Word expectedValue, @Slot(0) Word newValue) {
        return ptr.compareAndSwapWord(off, expectedValue, newValue);
    }

    @T1X_TEMPLATE(RETURN)
    public static void vreturn() {
    }

    @T1X_TEMPLATE(RETURN$unlock)
    public static void vreturnUnlock(Reference object) {
        Monitor.noninlineExit(object);
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$void)
    @Slot(-1)
    public static Address invokevirtualVoid(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectVirtualMethod(receiver, guard);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$void$resolved)
    @Slot(-1)
    public static Address invokevirtualVoid(int vTableIndex, Reference receiver) {
        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEVIRTUAL instruction.
     *
     * @param vTableIndex the index into the vtable of the virtual method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEVIRTUAL$void$instrumented)
    @Slot(-1)
    public static Address invokevirtualVoid(int vTableIndex, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves and selects the correct implementation of a method referenced by an INVOKEINTERFACE instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$void)
    @Slot(-1)
    public static Address invokeinterfaceVoid(ResolutionGuard.InPool guard, Reference receiver) {
        return resolveAndSelectInterfaceMethod(guard, receiver);
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$void$resolved)
    @Slot(-1)
    public static Address invokeinterfaceVoid(InterfaceMethodActor interfaceMethodActor, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Selects the correct implementation of a resolved method referenced by an INVOKEINTERFACE instruction.
     *
     * @param interfaceMethodActor the resolved interface method being invoked
     * @param mpo the profile object for an instrumented invocation
     * @param mpoIndex a profile specific index
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called
     */
    @T1X_TEMPLATE(INVOKEINTERFACE$void$instrumented)
    @Slot(-1)
    public static Address invokeinterfaceVoid(InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex, Reference receiver) {
        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).
            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    /**
     * Resolves a method referenced by an INVOKESPECIAL instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESPECIAL$void)
    @Slot(-1)
    public static Address invokespecialVoid(ResolutionGuard.InPool guard, Reference receiver) {
        nullCheck(receiver.toOrigin());
        return resolveSpecialMethod(guard);
    }

    /**
     * Resolves a method referenced by an INVOKESTATIC instruction.
     *
     * @param guard guard for a method symbol
     * @param receiver the receiver object of the invocation
     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked
     */
    @T1X_TEMPLATE(INVOKESTATIC$void)
    @Slot(-1)
    public static Address invokestaticVoid(ResolutionGuard.InPool guard) {
        return resolveStaticMethod(guard);
    }

    @T1X_TEMPLATE(WDIV)
    public static Address wdiv(@Slot(1) Address value1, @Slot(0) Address value2) {
        return value1.dividedBy(value2);
    }

    @T1X_TEMPLATE(WDIVI)
    public static Address wdivi(@Slot(1) Address value1, @Slot(0) int value2) {
        return value1.dividedBy(value2);
    }

    @T1X_TEMPLATE(WREM)
    public static Address wrem(@Slot(1) Address value1, @Slot(0) Address value2) {
        return value1.remainder(value2);
    }

    @T1X_TEMPLATE(WREMI)
    public static int wremi(@Slot(1) Address value1, @Slot(0) int value2) {
        return value1.remainder(value2);
    }

    @T1X_TEMPLATE(MOV_F2I)
    public static int mov_f2i(@Slot(0) float value) {
        return Intrinsics.floatToInt(value);
    }

    @T1X_TEMPLATE(MOV_I2F)
    public static float mov_i2f(@Slot(0) int value) {
        return Intrinsics.intToFloat(value);
    }

    @T1X_TEMPLATE(MOV_D2L)
    public static long mov_d2l(@Slot(0) double value) {
        return Intrinsics.doubleToLong(value);
    }

    @T1X_TEMPLATE(MOV_L2D)
    public static double mov_l2d(@Slot(0) long value) {
        return Intrinsics.longToDouble(value);
    }

    @T1X_TEMPLATE(NEW)
    public static Object new_(ResolutionGuard guard) {
        Object object = resolveClassForNewAndCreate(guard);
        return object;
    }

    @T1X_TEMPLATE(NEW$init)
    public static Object new_(ClassActor classActor) {
        Object object = createTupleOrHybrid(classActor);
        return object;
    }

    @T1X_TEMPLATE(NEWARRAY)
    public static Object newarray(Kind<?> kind, @Slot(0) int length) {
        Object array = createPrimitiveArray(kind, length);
        return array;
    }

    @T1X_TEMPLATE(ANEWARRAY)
    public static Object anewarray(ResolutionGuard arrayType, @Slot(0) int length) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(arrayType));
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        return array;
    }

    @T1X_TEMPLATE(ANEWARRAY$resolved)
    public static Object anewarray(ArrayClassActor<?> arrayType, @Slot(0) int length) {
        ArrayClassActor<?> arrayClassActor = arrayType;
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        return array;
    }

    @T1X_TEMPLATE(MULTIANEWARRAY)
    public static Reference multianewarray(ResolutionGuard guard, int[] lengths) {
        ClassActor arrayClassActor = Snippets.resolveClass(guard);
        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        return Reference.fromJava(array);
    }

    @T1X_TEMPLATE(MULTIANEWARRAY$resolved)
    public static Reference multianewarray(ArrayClassActor<?> arrayClassActor, int[] lengths) {
        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        return Reference.fromJava(array);
    }

    @T1X_TEMPLATE(CHECKCAST)
    public static Object checkcast(ResolutionGuard guard, @Slot(0) Object object) {
        resolveAndCheckcast(guard, object);
        return object;
    }

    @T1X_TEMPLATE(CHECKCAST$resolved)
    public static Object checkcast(ClassActor classActor, @Slot(0) Object object) {
        Snippets.checkCast(classActor, object);
        return object;
    }

    @NEVER_INLINE
    private static void resolveAndCheckcast(ResolutionGuard guard, final Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        Snippets.checkCast(classActor, object);
    }

    @T1X_TEMPLATE(ARRAYLENGTH)
    public static int arraylength(@Slot(0) Object array) {
        int length = ArrayAccess.readArrayLength(array);
        return length;
    }

    @T1X_TEMPLATE(ATHROW)
    public static void athrow(@Slot(0) Object object) {
        Throw.raise(object);
    }

    @T1X_TEMPLATE(MONITORENTER)
    public static void monitorenter(@Slot(0) Object object) {
        T1XRuntime.monitorenter(object);
    }

    @T1X_TEMPLATE(MONITOREXIT)
    public static void monitorexit(@Slot(0) Object object) {
        T1XRuntime.monitorexit(object);
    }

    @T1X_TEMPLATE(INSTANCEOF)
    public static int instanceof_(ResolutionGuard guard, @Slot(0) Object object) {
        ClassActor classActor = Snippets.resolveClass(guard);
        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));
    }

    @T1X_TEMPLATE(INSTANCEOF$resolved)
    public static int instanceof_(ClassActor classActor, @Slot(0) Object object) {
        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));
    }

    @T1X_TEMPLATE(RETURN$registerFinalizer)
    public static void vreturnRegisterFinalizer(Reference object) {
        if (ObjectAccess.readClassActor(object).hasFinalizer()) {
            SpecialReferenceManager.registerFinalizee(object);
        }
    }

    @T1X_TEMPLATE(TRACE_METHOD_ENTRY)
    public static void traceMethodEntry(String method) {
        Log.println(method);
    }

// END GENERATED CODE
}
