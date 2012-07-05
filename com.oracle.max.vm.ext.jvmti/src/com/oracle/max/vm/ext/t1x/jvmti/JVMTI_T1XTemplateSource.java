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
package com.oracle.max.vm.ext.t1x.jvmti;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.oracle.max.vm.ext.t1x.T1XRuntime.*;

import com.oracle.max.vm.ext.t1x.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;


public class JVMTI_T1XTemplateSource {
// START GENERATED CODE
    @T1X_TEMPLATE(GETFIELD$boolean$resolved)
    public static int getfieldBoolean(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$byte$resolved)
    public static int getfieldByte(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$char$resolved)
    public static int getfieldChar(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$short$resolved)
    public static int getfieldShort(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$int$resolved)
    public static int getfieldInt(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$float$resolved)
    public static float getfieldFloat(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$long$resolved)
    public static long getfieldLong(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$double$resolved)
    public static double getfieldDouble(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$reference$resolved)
    public static Reference getfieldObject(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(GETFIELD$word$resolved)
    public static Word getfieldWord(@Slot(0) Object object, int offset) {
        JVMTI.fieldAccessEvent(object, offset, false);
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
        JVMTI.fieldAccessEvent(object, f.offset(), false);
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

    @T1X_TEMPLATE(PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(@Slot(1) Object object, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeBoolean(object, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldBoolean(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(object, f.offset(), UnsafeCast.asBoolean((byte) value));
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(object, f.offset(), UnsafeCast.asBoolean((byte) value));
        }
    }

    @T1X_TEMPLATE(PUTFIELD$byte$resolved)
    public static void putfieldByte(@Slot(1) Object object, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeByte(object, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldByte(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(object, f.offset(), (byte) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(object, f.offset(), (byte) value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$char$resolved)
    public static void putfieldChar(@Slot(1) Object object, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeChar(object, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldChar(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(object, f.offset(), (char) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(object, f.offset(), (char) value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$short$resolved)
    public static void putfieldShort(@Slot(1) Object object, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeShort(object, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldShort(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(object, f.offset(), (short) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(object, f.offset(), (short) value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$int$resolved)
    public static void putfieldInt(@Slot(1) Object object, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeInt(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) int value) {
        resolveAndPutFieldInt(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$float$resolved)
    public static void putfieldFloat(@Slot(1) Object object, int offset, @Slot(0) float value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeFloat(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) float value) {
        resolveAndPutFieldFloat(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$long$resolved)
    public static void putfieldLong(@Slot(2) Object object, int offset, @Slot(0) long value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeLong(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) long value) {
        resolveAndPutFieldLong(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$double$resolved)
    public static void putfieldDouble(@Slot(2) Object object, int offset, @Slot(0) double value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.writeDouble(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard, @Slot(2) Object object, @Slot(0) double value) {
        resolveAndPutFieldDouble(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$reference$resolved)
    public static void putfieldReference(@Slot(1) Object object, int offset, @Slot(0) Reference value) {
        JVMTI.fieldModificationEvent(object, offset, false, value);
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Reference value) {
        resolveAndPutFieldReference(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Reference value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(object, f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTFIELD$word$resolved)
    public static void putfieldWord(@Slot(1) Object object, int offset, @Slot(0) Word value) {
        JVMTI.fieldModificationEvent(object, offset, false, value.asAddress().toLong());
        TupleAccess.writeWord(object, offset, value);
    }

    @T1X_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard, @Slot(1) Object object, @Slot(0) Word value) {
        resolveAndPutFieldWord(guard, object, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        JVMTI.fieldModificationEvent(object, f.offset(), false, value.asAddress().toLong());
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(object, f.offset(), value);
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
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        boolean result = TupleAccess.readBoolean(staticTuple, offset);
        return UnsafeCast.asByte(result);
    }

    @T1X_TEMPLATE(GETSTATIC$byte)
    public static int getstaticByte(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticByte(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        byte result = TupleAccess.readByte(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$char)
    public static int getstaticChar(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticChar(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        char result = TupleAccess.readChar(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$short)
    public static int getstaticShort(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticShort(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        short result = TupleAccess.readShort(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$int)
    public static int getstaticInt(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticInt(guard);
    }

    @NEVER_INLINE
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        int result = TupleAccess.readInt(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$float)
    public static float getstaticFloat(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticFloat(guard);
    }

    @NEVER_INLINE
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        float result = TupleAccess.readFloat(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$long)
    public static long getstaticLong(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticLong(guard);
    }

    @NEVER_INLINE
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        long result = TupleAccess.readLong(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$double)
    public static double getstaticDouble(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticDouble(guard);
    }

    @NEVER_INLINE
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        double result = TupleAccess.readDouble(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(GETSTATIC$reference)
    public static Reference getstaticReference(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticReference(guard);
    }

    @NEVER_INLINE
    public static Reference resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        Object result = TupleAccess.readObject(staticTuple, offset);
        return Reference.fromJava(result);
    }

    @T1X_TEMPLATE(GETSTATIC$word)
    public static Word getstaticWord(ResolutionGuard.InPool guard) {
        return resolveAndGetStaticWord(guard);
    }

    @NEVER_INLINE
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        JVMTI.fieldAccessEvent(f.holder().staticTuple(), f.offset(), true);
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
        JVMTI.fieldAccessEvent(staticTuple, offset, true);
        Word result = TupleAccess.readWord(staticTuple, offset);
        return result;
    }

    @T1X_TEMPLATE(PUTSTATIC$boolean$init)
    public static void putstaticBoolean(Object staticTuple, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeBoolean(staticTuple, offset, UnsafeCast.asBoolean((byte) value));
    }

    @T1X_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticBoolean(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), UnsafeCast.asBoolean((byte) value));
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), UnsafeCast.asBoolean((byte) value));
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$byte$init)
    public static void putstaticByte(Object staticTuple, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeByte(staticTuple, offset, (byte) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticByte(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), (byte) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), (byte) value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$char$init)
    public static void putstaticChar(Object staticTuple, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeChar(staticTuple, offset, (char) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticChar(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), (char) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), (char) value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$short$init)
    public static void putstaticShort(Object staticTuple, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeShort(staticTuple, offset, (short) value);
    }

    @T1X_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticShort(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), (short) value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), (short) value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$int$init)
    public static void putstaticInt(Object staticTuple, int offset, @Slot(0) int value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard, @Slot(0) int value) {
        resolveAndPutStaticInt(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$float$init)
    public static void putstaticFloat(Object staticTuple, int offset, @Slot(0) float value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard, @Slot(0) float value) {
        resolveAndPutStaticFloat(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$long$init)
    public static void putstaticLong(Object staticTuple, int offset, @Slot(0) long value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard, @Slot(0) long value) {
        resolveAndPutStaticLong(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$double$init)
    public static void putstaticDouble(Object staticTuple, int offset, @Slot(0) double value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard, @Slot(0) double value) {
        resolveAndPutStaticDouble(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$reference$init)
    public static void putstaticReference(Object staticTuple, int offset, @Slot(0) Reference value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value);
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard, @Slot(0) Reference value) {
        resolveAndPutStaticReference(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Reference value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(PUTSTATIC$word$init)
    public static void putstaticWord(Object staticTuple, int offset, @Slot(0) Word value) {
        JVMTI.fieldModificationEvent(staticTuple, offset, true, value.asAddress().toLong());
        TupleAccess.writeWord(staticTuple, offset, value);
    }

    @T1X_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard, @Slot(0) Word value) {
        resolveAndPutStaticWord(guard, value);
    }

    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        JVMTI.fieldModificationEvent(f.holder().staticTuple(), f.offset(), true, value.asAddress().toLong());
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
        }
    }

    @T1X_TEMPLATE(TRACE_METHOD_ENTRY)
    public static void traceMethodEntry(MethodActor methodActor) {
        JVMTI.event(JVMTIEvent.METHOD_ENTRY, methodActor);
    }

    @T1X_TEMPLATE(BREAKPOINT)
    public static void breakpoint(long id) {
        JVMTIBreakpoints.event(id);
    }

    @T1X_TEMPLATE(IRETURN)
    @Slot(-1)
    public static int ireturn(@Slot(0) int value) {
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(IRETURN$unlock)
    @Slot(-1)
    public static int ireturnUnlock(Reference object, @Slot(0) int value) {
        Monitor.noninlineExit(object);
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(FRETURN)
    @Slot(-1)
    public static float freturn(@Slot(0) float value) {
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(FRETURN$unlock)
    @Slot(-1)
    public static float freturnUnlock(Reference object, @Slot(0) float value) {
        Monitor.noninlineExit(object);
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(LRETURN)
    @Slot(-1)
    public static long lreturn(@Slot(0) long value) {
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(LRETURN$unlock)
    @Slot(-1)
    public static long lreturnUnlock(Reference object, @Slot(0) long value) {
        Monitor.noninlineExit(object);
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(DRETURN)
    @Slot(-1)
    public static double dreturn(@Slot(0) double value) {
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(DRETURN$unlock)
    @Slot(-1)
    public static double dreturnUnlock(Reference object, @Slot(0) double value) {
        Monitor.noninlineExit(object);
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(ARETURN)
    @Slot(-1)
    public static Reference areturn(@Slot(0) Reference value) {
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(ARETURN$unlock)
    @Slot(-1)
    public static Reference areturnUnlock(Reference object, @Slot(0) Reference value) {
        Monitor.noninlineExit(object);
        JVMTIThreadFunctions.framePopEvent(false, value);
        return value;
    }

    @T1X_TEMPLATE(RETURN)
    @Slot(-1)
    public static void vreturn() {
        JVMTIThreadFunctions.framePopEvent(false);
    }

    @T1X_TEMPLATE(RETURN$unlock)
    @Slot(-1)
    public static void vreturnUnlock(Reference object) {
        Monitor.noninlineExit(object);
        JVMTIThreadFunctions.framePopEvent(false);
    }

// END GENERATED CODE
}
