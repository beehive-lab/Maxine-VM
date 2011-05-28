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

import static com.sun.max.vm.t1x.T1XTemplateTag.*;
import static com.sun.max.vm.t1x.T1XFrameOps.*;
import static com.sun.max.vm.t1x.T1XRuntime.*;
import static com.sun.max.vm.t1x.T1XTemplateSource.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.*;
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

import com.oracle.max.vm.ext.vma.runtime.*;


public class VMACountBytecodesTemplateSource {

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$boolean$resolved)
    public static void getfieldBoolean(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeBoolean(0, TupleAccess.readBoolean(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$boolean)
    public static void getfieldBoolean(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeBoolean(0, resolveAndGetFieldBoolean(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static boolean resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readBoolean(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$boolean)
    public static void getstaticBoolean(ResolutionGuard.InPool guard) {
        pushBoolean(resolveAndGetStaticBoolean(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static boolean resolveAndGetStaticBoolean(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$boolean$init)
    public static void getstaticBoolean(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushBoolean(TupleAccess.readBoolean(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(int offset) {
        Object object = peekObject(1);
        boolean value = peekBoolean(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeBoolean(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        boolean value = peekBoolean(0);
        resolveAndPutFieldBoolean(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, boolean value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$boolean$init)
    public static void putstaticBoolean(Object staticTuple, int offset) {
        boolean value = popBoolean();
        BytecodeCounter.inc(179);
        TupleAccess.writeBoolean(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard) {
        resolveAndPutStaticBoolean(guard, popBoolean());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, boolean value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(BALOAD)
    public static void baload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(51);
        removeSlots(1);
        pokeBoolean(0, ArrayAccess.getBoolean(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(BASTORE)
    public static void bastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        boolean value = peekBoolean(0);
        BytecodeCounter.inc(84);
        ArrayAccess.setBoolean(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$byte$resolved)
    public static void getfieldByte(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeByte(0, TupleAccess.readByte(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$byte)
    public static void getfieldByte(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeByte(0, resolveAndGetFieldByte(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static byte resolveAndGetFieldByte(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readByte(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$byte)
    public static void getstaticByte(ResolutionGuard.InPool guard) {
        pushByte(resolveAndGetStaticByte(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static byte resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readByte(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$byte$init)
    public static void getstaticByte(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushByte(TupleAccess.readByte(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$byte$resolved)
    public static void putfieldByte(int offset) {
        Object object = peekObject(1);
        byte value = peekByte(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeByte(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        byte value = peekByte(0);
        resolveAndPutFieldByte(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, byte value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$byte$init)
    public static void putstaticByte(Object staticTuple, int offset) {
        byte value = popByte();
        BytecodeCounter.inc(179);
        TupleAccess.writeByte(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard) {
        resolveAndPutStaticByte(guard, popByte());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, byte value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(I2B)
    public static void i2b() {
        int value = peekInt(0);
        BytecodeCounter.inc(145);
        pokeByte(0, (byte) value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_BYTE)
    public static void pget_byte() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(481);
        removeSlots(2);
        pokeByte(0, ptr.getByte(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_BYTE)
    public static void pset_byte() {
        byte value = peekByte(0);
        int index = peekInt(1);
        int disp = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(482);
        removeSlots(4);
        ptr.setByte(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_BYTE)
    public static void pread_byte() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(479);
        removeSlots(1);
        pokeByte(0, ptr.readByte(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_BYTE)
    public static void pwrite_byte() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        byte value = peekByte(0);
        BytecodeCounter.inc(480);
        removeSlots(3);
        ptr.writeByte(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_BYTE_I)
    public static void pread_byte_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(2783);
        removeSlots(1);
        pokeByte(0, ptr.readByte(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_BYTE_I)
    public static void pwrite_byte_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        byte value = peekByte(0);
        BytecodeCounter.inc(2528);
        removeSlots(3);
        ptr.writeByte(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$char$resolved)
    public static void getfieldChar(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeChar(0, TupleAccess.readChar(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$char)
    public static void getfieldChar(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeChar(0, resolveAndGetFieldChar(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static char resolveAndGetFieldChar(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readChar(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$char)
    public static void getstaticChar(ResolutionGuard.InPool guard) {
        pushChar(resolveAndGetStaticChar(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static char resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readChar(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$char$init)
    public static void getstaticChar(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushChar(TupleAccess.readChar(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$char$resolved)
    public static void putfieldChar(int offset) {
        Object object = peekObject(1);
        char value = peekChar(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeChar(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        char value = peekChar(0);
        resolveAndPutFieldChar(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, char value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$char$init)
    public static void putstaticChar(Object staticTuple, int offset) {
        char value = popChar();
        BytecodeCounter.inc(179);
        TupleAccess.writeChar(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard) {
        resolveAndPutStaticChar(guard, popChar());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, char value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(I2C)
    public static void i2c() {
        int value = peekInt(0);
        BytecodeCounter.inc(146);
        pokeChar(0, (char) value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(CALOAD)
    public static void caload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(52);
        removeSlots(1);
        pokeChar(0, ArrayAccess.getChar(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(CASTORE)
    public static void castore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        char value = peekChar(0);
        BytecodeCounter.inc(85);
        ArrayAccess.setChar(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_CHAR)
    public static void pget_char() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(737);
        removeSlots(2);
        pokeChar(0, ptr.getChar(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_CHAR)
    public static void pread_char() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(735);
        removeSlots(1);
        pokeChar(0, ptr.readChar(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_CHAR_I)
    public static void pread_char_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(3039);
        removeSlots(1);
        pokeChar(0, ptr.readChar(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$short$resolved)
    public static void getfieldShort(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeShort(0, TupleAccess.readShort(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$short)
    public static void getfieldShort(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeShort(0, resolveAndGetFieldShort(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static short resolveAndGetFieldShort(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readShort(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$short)
    public static void getstaticShort(ResolutionGuard.InPool guard) {
        pushShort(resolveAndGetStaticShort(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static short resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readShort(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$short$init)
    public static void getstaticShort(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushShort(TupleAccess.readShort(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$short$resolved)
    public static void putfieldShort(int offset) {
        Object object = peekObject(1);
        short value = peekShort(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeShort(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        short value = peekShort(0);
        resolveAndPutFieldShort(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, short value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$short$init)
    public static void putstaticShort(Object staticTuple, int offset) {
        short value = popShort();
        BytecodeCounter.inc(179);
        TupleAccess.writeShort(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard) {
        resolveAndPutStaticShort(guard, popShort());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, short value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(I2S)
    public static void i2s() {
        int value = peekInt(0);
        BytecodeCounter.inc(147);
        pokeShort(0, (short) value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(SALOAD)
    public static void saload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(53);
        removeSlots(1);
        pokeShort(0, ArrayAccess.getShort(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(SASTORE)
    public static void sastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        short value = peekShort(0);
        BytecodeCounter.inc(86);
        ArrayAccess.setShort(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_SHORT)
    public static void pget_short() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(993);
        removeSlots(2);
        pokeShort(0, ptr.getShort(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_SHORT)
    public static void pset_short() {
        short value = peekShort(0);
        int index = peekInt(1);
        int disp = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(738);
        removeSlots(4);
        ptr.setShort(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_SHORT)
    public static void pread_short() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(991);
        removeSlots(1);
        pokeShort(0, ptr.readShort(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_SHORT)
    public static void pwrite_short() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        short value = peekShort(0);
        BytecodeCounter.inc(736);
        removeSlots(3);
        ptr.writeShort(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_SHORT_I)
    public static void pread_short_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(3295);
        removeSlots(1);
        pokeShort(0, ptr.readShort(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_SHORT_I)
    public static void pwrite_short_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        short value = peekShort(0);
        BytecodeCounter.inc(2784);
        removeSlots(3);
        ptr.writeShort(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_0)
    public static void iconst_0() {
        BytecodeCounter.inc(3);
        pushInt(0);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_1)
    public static void iconst_1() {
        BytecodeCounter.inc(4);
        pushInt(1);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_2)
    public static void iconst_2() {
        BytecodeCounter.inc(5);
        pushInt(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_3)
    public static void iconst_3() {
        BytecodeCounter.inc(6);
        pushInt(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_4)
    public static void iconst_4() {
        BytecodeCounter.inc(7);
        pushInt(4);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_5)
    public static void iconst_5() {
        BytecodeCounter.inc(8);
        pushInt(5);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ICONST_M1)
    public static void iconst_m1() {
        BytecodeCounter.inc(2);
        pushInt(-1);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$int$resolved)
    public static void getfieldInt(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeInt(0, TupleAccess.readInt(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$int)
    public static void getfieldInt(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeInt(0, resolveAndGetFieldInt(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readInt(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$int)
    public static void getstaticInt(ResolutionGuard.InPool guard) {
        pushInt(resolveAndGetStaticInt(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readInt(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$int$init)
    public static void getstaticInt(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushInt(TupleAccess.readInt(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$int$resolved)
    public static void putfieldInt(int offset) {
        Object object = peekObject(1);
        int value = peekInt(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeInt(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        int value = peekInt(0);
        resolveAndPutFieldInt(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$int$init)
    public static void putstaticInt(Object staticTuple, int offset) {
        int value = popInt();
        BytecodeCounter.inc(179);
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard) {
        resolveAndPutStaticInt(guard, popInt());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeInt(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ILOAD)
    public static void iload(int dispToLocalSlot) {
        int value = getLocalInt(dispToLocalSlot);
        BytecodeCounter.inc(21);
        pushInt(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ISTORE)
    public static void istore(int dispToLocalSlot) {
        int value = popInt();
        BytecodeCounter.inc(54);
        setLocalInt(dispToLocalSlot, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDC$int)
    public static void ildc(int constant) {
        BytecodeCounter.inc(18);
        pushInt(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(L2I)
    public static void l2i() {
        long value = peekLong(0);
        removeSlots(1);
        BytecodeCounter.inc(136);
        pokeInt(0, (int) value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(F2I)
    public static void f2i() {
        float value = peekFloat(0);
        BytecodeCounter.inc(139);
        pokeInt(0, T1XRuntime.f2i(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(D2I)
    public static void d2i() {
        double value = peekDouble(0);
        removeSlots(1);
        BytecodeCounter.inc(142);
        pokeInt(0, T1XRuntime.d2i(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IADD)
    public static void iadd() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(96);
        removeSlots(1);
        pokeInt(0, value1 + value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ISUB)
    public static void isub() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(100);
        removeSlots(1);
        pokeInt(0, value1 - value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IMUL)
    public static void imul() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(104);
        removeSlots(1);
        pokeInt(0, value1 * value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IDIV)
    public static void idiv() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(108);
        removeSlots(1);
        pokeInt(0, value1 / value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IREM)
    public static void irem() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(112);
        removeSlots(1);
        pokeInt(0, value1 % value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INEG)
    public static void ineg() {
        int value = -peekInt(0);
        BytecodeCounter.inc(116);
        pokeInt(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IOR)
    public static void ior() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(128);
        removeSlots(1);
        pokeInt(0, value1 | value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IAND)
    public static void iand() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(126);
        removeSlots(1);
        pokeInt(0, value1 & value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IXOR)
    public static void ixor() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(130);
        removeSlots(1);
        pokeInt(0, value1 ^ value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ISHL)
    public static void ishl() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(120);
        removeSlots(1);
        pokeInt(0, value1 << value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ISHR)
    public static void ishr() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(122);
        removeSlots(1);
        pokeInt(0, value1 >> value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IUSHR)
    public static void iushr() {
        int value2 = peekInt(0);
        int value1 = peekInt(1);
        BytecodeCounter.inc(124);
        removeSlots(1);
        pokeInt(0, value1 >>> value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ICMPEQ)
    public static void if_icmpeq() {
        BytecodeCounter.inc(159);
        icmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFEQ)
    public static void ifeq() {
        BytecodeCounter.inc(153);
        icmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ICMPNE)
    public static void if_icmpne() {
        BytecodeCounter.inc(160);
        icmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFNE)
    public static void ifne() {
        BytecodeCounter.inc(154);
        icmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ICMPLT)
    public static void if_icmplt() {
        BytecodeCounter.inc(161);
        icmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFLT)
    public static void iflt() {
        BytecodeCounter.inc(155);
        icmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ICMPGE)
    public static void if_icmpge() {
        BytecodeCounter.inc(162);
        icmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFGE)
    public static void ifge() {
        BytecodeCounter.inc(156);
        icmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ICMPGT)
    public static void if_icmpgt() {
        BytecodeCounter.inc(163);
        icmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFGT)
    public static void ifgt() {
        BytecodeCounter.inc(157);
        icmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ICMPLE)
    public static void if_icmple() {
        BytecodeCounter.inc(164);
        icmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFLE)
    public static void ifle() {
        BytecodeCounter.inc(158);
        icmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IRETURN)
    public static int ireturn() {
        BytecodeCounter.inc(172);
        return popInt();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IRETURN$unlockClass)
    public static int ireturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(172);
        Monitor.noninlineExit(rcvr);
        return popInt();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IRETURN$unlockReceiver)
    public static int ireturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(172);
        Monitor.noninlineExit(rcvr);
        return popInt();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IALOAD)
    public static void iaload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(46);
        removeSlots(1);
        pokeInt(0, ArrayAccess.getInt(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IASTORE)
    public static void iastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        int value = peekInt(0);
        BytecodeCounter.inc(79);
        ArrayAccess.setInt(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_INT)
    public static void pget_int() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(1249);
        removeSlots(2);
        pokeInt(0, ptr.getInt(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_INT)
    public static void pset_int() {
        int value = peekInt(0);
        int index = peekInt(1);
        int disp = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(994);
        removeSlots(4);
        ptr.setInt(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_INT)
    public static void pread_int() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(1247);
        removeSlots(1);
        pokeInt(0, ptr.readInt(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_INT)
    public static void pwrite_int() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        int value = peekInt(0);
        BytecodeCounter.inc(992);
        removeSlots(3);
        ptr.writeInt(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_INT_I)
    public static void pread_int_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(3551);
        removeSlots(1);
        pokeInt(0, ptr.readInt(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_INT_I)
    public static void pwrite_int_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        int value = peekInt(0);
        BytecodeCounter.inc(3040);
        removeSlots(3);
        ptr.writeInt(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PCMPSWP_INT)
    public static void pcmpswp_int() {
        int newValue = peekInt(0);
        int expectedValue = peekInt(1);
        Offset off = peekWord(2).asOffset();
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(483);
        removeSlots(3);
        pokeInt(0, ptr.compareAndSwapInt(off, expectedValue, newValue));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PCMPSWP_INT_I)
    public static void pcmpswp_int_i() {
        int newValue = peekInt(0);
        int expectedValue = peekInt(1);
        int off = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(1507);
        removeSlots(3);
        pokeInt(0, ptr.compareAndSwapInt(off, expectedValue, newValue));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FCONST)
    public static void fconst(float constant) {
        BytecodeCounter.inc(11);
        pushFloat(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$float$resolved)
    public static void getfieldFloat(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeFloat(0, TupleAccess.readFloat(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$float)
    public static void getfieldFloat(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeFloat(0, resolveAndGetFieldFloat(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readFloat(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$float)
    public static void getstaticFloat(ResolutionGuard.InPool guard) {
        pushFloat(resolveAndGetStaticFloat(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$float$init)
    public static void getstaticFloat(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushFloat(TupleAccess.readFloat(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$float$resolved)
    public static void putfieldFloat(int offset) {
        Object object = peekObject(1);
        float value = peekFloat(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeFloat(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        float value = peekFloat(0);
        resolveAndPutFieldFloat(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$float$init)
    public static void putstaticFloat(Object staticTuple, int offset) {
        float value = popFloat();
        BytecodeCounter.inc(179);
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard) {
        resolveAndPutStaticFloat(guard, popFloat());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeFloat(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FLOAD)
    public static void fload(int dispToLocalSlot) {
        float value = getLocalFloat(dispToLocalSlot);
        BytecodeCounter.inc(23);
        pushFloat(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FSTORE)
    public static void fstore(int dispToLocalSlot) {
        float value = popFloat();
        BytecodeCounter.inc(56);
        setLocalFloat(dispToLocalSlot, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDC$float)
    public static void fldc(float constant) {
        BytecodeCounter.inc(18);
        pushFloat(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(I2F)
    public static void i2f() {
        int value = peekInt(0);
        BytecodeCounter.inc(134);
        pokeFloat(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(L2F)
    public static void l2f() {
        long value = peekLong(0);
        removeSlots(1);
        BytecodeCounter.inc(137);
        pokeFloat(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(D2F)
    public static void d2f() {
        double value = peekDouble(0);
        removeSlots(1);
        BytecodeCounter.inc(144);
        pokeFloat(0, (float) value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FADD)
    public static void fadd() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        BytecodeCounter.inc(98);
        removeSlots(1);
        pokeFloat(0, value1 + value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FSUB)
    public static void fsub() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        BytecodeCounter.inc(102);
        removeSlots(1);
        pokeFloat(0, value1 - value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FMUL)
    public static void fmul() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        BytecodeCounter.inc(106);
        removeSlots(1);
        pokeFloat(0, value1 * value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FDIV)
    public static void fdiv() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        BytecodeCounter.inc(110);
        removeSlots(1);
        pokeFloat(0, value1 / value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FREM)
    public static void frem() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        BytecodeCounter.inc(114);
        removeSlots(1);
        pokeFloat(0, value1 % value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FNEG)
    public static void fneg(float zero) {
        float value = zero - peekFloat(0);
        BytecodeCounter.inc(118);
        pokeFloat(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FCMPG)
    public static void fcmpgOp() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        int result = fcmpg(value1, value2);
        BytecodeCounter.inc(150);
        removeSlots(1);
        pokeInt(0, result);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FCMPL)
    public static void fcmplOp() {
        float value2 = peekFloat(0);
        float value1 = peekFloat(1);
        int result = fcmpl(value1, value2);
        BytecodeCounter.inc(149);
        removeSlots(1);
        pokeInt(0, result);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FRETURN)
    public static float freturn() {
        BytecodeCounter.inc(174);
        return popFloat();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FRETURN$unlockClass)
    public static float freturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(174);
        Monitor.noninlineExit(rcvr);
        return popFloat();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FRETURN$unlockReceiver)
    public static float freturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(174);
        Monitor.noninlineExit(rcvr);
        return popFloat();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FALOAD)
    public static void faload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(48);
        removeSlots(1);
        pokeFloat(0, ArrayAccess.getFloat(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FASTORE)
    public static void fastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        float value = peekFloat(0);
        BytecodeCounter.inc(81);
        ArrayAccess.setFloat(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$float)
    public static void invokevirtualFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$float$resolved)
    public static void invokevirtualFloat(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$float$instrumented)
    public static void invokevirtualFloat(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$float)
    public static void invokeinterfaceFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$float$resolved)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$float$instrumented)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$float)
    public static void invokespecialFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        indirectCallFloat(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$float$resolved)
    public static void invokespecialFloat(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        directCallFloat();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$float)
    public static void invokestaticFloat(ResolutionGuard.InPool guard) {
        BytecodeCounter.inc(184);
        indirectCallFloat(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$float$init)
    public static void invokestaticFloat() {
        BytecodeCounter.inc(184);
        directCallFloat();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_FLOAT)
    public static void pget_float() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(1505);
        removeSlots(2);
        pokeFloat(0, ptr.getFloat(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_FLOAT)
    public static void pset_float() {
        float value = peekFloat(0);
        int index = peekInt(1);
        int disp = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(1250);
        removeSlots(4);
        ptr.setFloat(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_FLOAT)
    public static void pread_float() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(1503);
        removeSlots(1);
        pokeFloat(0, ptr.readFloat(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_FLOAT)
    public static void pwrite_float() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        float value = peekFloat(0);
        BytecodeCounter.inc(1248);
        removeSlots(3);
        ptr.writeFloat(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_FLOAT_I)
    public static void pread_float_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(3807);
        removeSlots(1);
        pokeFloat(0, ptr.readFloat(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_FLOAT_I)
    public static void pwrite_float_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        float value = peekFloat(0);
        BytecodeCounter.inc(3296);
        removeSlots(3);
        ptr.writeFloat(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LCONST)
    public static void lconst(long constant) {
        BytecodeCounter.inc(9);
        pushLong(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$long$resolved)
    public static void getfieldLong(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        addSlots(1);
        pokeLong(0, TupleAccess.readLong(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$long)
    public static void getfieldLong(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        addSlots(1);
        pokeLong(0, resolveAndGetFieldLong(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readLong(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$long)
    public static void getstaticLong(ResolutionGuard.InPool guard) {
        pushLong(resolveAndGetStaticLong(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readLong(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$long$init)
    public static void getstaticLong(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushLong(TupleAccess.readLong(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$long$resolved)
    public static void putfieldLong(int offset) {
        Object object = peekObject(2);
        long value = peekLong(0);
        BytecodeCounter.inc(181);
        removeSlots(3);
        TupleAccess.writeLong(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard) {
        Object object = peekObject(2);
        long value = peekLong(0);
        resolveAndPutFieldLong(guard, object, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$long$init)
    public static void putstaticLong(Object staticTuple, int offset) {
        long value = popLong();
        BytecodeCounter.inc(179);
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard) {
        resolveAndPutStaticLong(guard, popLong());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeLong(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LLOAD)
    public static void lload(int dispToLocalSlot) {
        long value = getLocalLong(dispToLocalSlot);
        BytecodeCounter.inc(22);
        pushLong(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LSTORE)
    public static void lstore(int dispToLocalSlot) {
        long value = popLong();
        BytecodeCounter.inc(55);
        setLocalLong(dispToLocalSlot, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDC$long)
    public static void lldc(long constant) {
        BytecodeCounter.inc(18);
        pushLong(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(I2L)
    public static void i2l() {
        int value = peekInt(0);
        addSlots(1);
        BytecodeCounter.inc(133);
        pokeLong(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(F2L)
    public static void f2l() {
        float value = peekFloat(0);
        addSlots(1);
        BytecodeCounter.inc(140);
        pokeLong(0, T1XRuntime.f2l(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(D2L)
    public static void d2l() {
        double value = peekDouble(0);
        BytecodeCounter.inc(143);
        pokeLong(0, T1XRuntime.d2l(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LADD)
    public static void ladd() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(97);
        removeSlots(2);
        pokeLong(0, value1 + value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LSUB)
    public static void lsub() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(101);
        removeSlots(2);
        pokeLong(0, value1 - value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LMUL)
    public static void lmul() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(105);
        removeSlots(2);
        pokeLong(0, value1 * value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDIV)
    public static void ldiv() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(109);
        removeSlots(2);
        pokeLong(0, value1 / value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LREM)
    public static void lrem() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(113);
        removeSlots(2);
        pokeLong(0, value1 % value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LNEG)
    public static void lneg() {
        long value = -peekLong(0);
        BytecodeCounter.inc(117);
        pokeLong(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LOR)
    public static void lor() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(129);
        removeSlots(2);
        pokeLong(0, value1 | value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LAND)
    public static void land() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(127);
        removeSlots(2);
        pokeLong(0, value1 & value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LXOR)
    public static void lxor() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        BytecodeCounter.inc(131);
        removeSlots(2);
        pokeLong(0, value1 ^ value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LSHL)
    public static void lshl() {
        int value2 = peekInt(0);
        long value1 = peekLong(1);
        BytecodeCounter.inc(121);
        removeSlots(1);
        pokeLong(0, value1 << value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LSHR)
    public static void lshr() {
        int value2 = peekInt(0);
        long value1 = peekLong(1);
        BytecodeCounter.inc(123);
        removeSlots(1);
        pokeLong(0, value1 >> value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LUSHR)
    public static void lushr() {
        int value2 = peekInt(0);
        long value1 = peekLong(1);
        BytecodeCounter.inc(125);
        removeSlots(1);
        pokeLong(0, value1 >>> value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LCMP)
    public static void lcmpOp() {
        long value2 = peekLong(0);
        long value1 = peekLong(2);
        int result = lcmp(value1, value2);
        BytecodeCounter.inc(148);
        removeSlots(3);
        pokeInt(0, result);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LRETURN)
    public static long lreturn() {
        BytecodeCounter.inc(173);
        return popLong();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LRETURN$unlockClass)
    public static long lreturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(173);
        Monitor.noninlineExit(rcvr);
        return popLong();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LRETURN$unlockReceiver)
    public static long lreturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(173);
        Monitor.noninlineExit(rcvr);
        return popLong();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LALOAD)
    public static void laload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(47);
        pokeLong(0, ArrayAccess.getLong(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LASTORE)
    public static void lastore() {
        int index = peekInt(2);
        Object array = peekObject(3);
        ArrayAccess.checkIndex(array, index);
        long value = peekLong(0);
        BytecodeCounter.inc(80);
        ArrayAccess.setLong(array, index, value);
        removeSlots(4);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$long)
    public static void invokevirtualLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$long$resolved)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$long$instrumented)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$long)
    public static void invokeinterfaceLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$long$resolved)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$long$instrumented)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$long)
    public static void invokespecialLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        indirectCallLong(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$long$resolved)
    public static void invokespecialLong(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        directCallLong();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$long)
    public static void invokestaticLong(ResolutionGuard.InPool guard) {
        BytecodeCounter.inc(184);
        indirectCallLong(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$long$init)
    public static void invokestaticLong() {
        BytecodeCounter.inc(184);
        directCallLong();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_LONG)
    public static void pget_long() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(1761);
        removeSlots(1);
        pokeLong(0, ptr.getLong(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_LONG)
    public static void pset_long() {
        long value = peekLong(0);
        int index = peekInt(2);
        int disp = peekInt(3);
        Pointer ptr = peekWord(4).asPointer();
        BytecodeCounter.inc(1506);
        removeSlots(5);
        ptr.setLong(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_LONG)
    public static void pread_long() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(1759);
        pokeLong(0, ptr.readLong(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_LONG)
    public static void pwrite_long() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        long value = peekLong(0);
        BytecodeCounter.inc(1504);
        removeSlots(4);
        ptr.writeLong(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_LONG_I)
    public static void pread_long_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(4063);
        pokeLong(0, ptr.readLong(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_LONG_I)
    public static void pwrite_long_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        long value = peekLong(0);
        BytecodeCounter.inc(3552);
        removeSlots(4);
        ptr.writeLong(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DCONST)
    public static void dconst(double constant) {
        BytecodeCounter.inc(14);
        pushDouble(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$double$resolved)
    public static void getfieldDouble(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        addSlots(1);
        pokeDouble(0, TupleAccess.readDouble(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$double)
    public static void getfieldDouble(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        addSlots(1);
        pokeDouble(0, resolveAndGetFieldDouble(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readDouble(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$double)
    public static void getstaticDouble(ResolutionGuard.InPool guard) {
        pushDouble(resolveAndGetStaticDouble(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$double$init)
    public static void getstaticDouble(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushDouble(TupleAccess.readDouble(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$double$resolved)
    public static void putfieldDouble(int offset) {
        Object object = peekObject(2);
        double value = peekDouble(0);
        BytecodeCounter.inc(181);
        removeSlots(3);
        TupleAccess.writeDouble(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard) {
        Object object = peekObject(2);
        double value = peekDouble(0);
        resolveAndPutFieldDouble(guard, object, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$double$init)
    public static void putstaticDouble(Object staticTuple, int offset) {
        double value = popDouble();
        BytecodeCounter.inc(179);
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard) {
        resolveAndPutStaticDouble(guard, popDouble());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeDouble(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DLOAD)
    public static void dload(int dispToLocalSlot) {
        double value = getLocalDouble(dispToLocalSlot);
        BytecodeCounter.inc(24);
        pushDouble(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DSTORE)
    public static void dstore(int dispToLocalSlot) {
        double value = popDouble();
        BytecodeCounter.inc(57);
        setLocalDouble(dispToLocalSlot, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDC$double)
    public static void dldc(double constant) {
        BytecodeCounter.inc(18);
        pushDouble(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(I2D)
    public static void i2d() {
        int value = peekInt(0);
        addSlots(1);
        BytecodeCounter.inc(135);
        pokeDouble(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(L2D)
    public static void l2d() {
        long value = peekLong(0);
        BytecodeCounter.inc(138);
        pokeDouble(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(F2D)
    public static void f2d() {
        float value = peekFloat(0);
        addSlots(1);
        BytecodeCounter.inc(141);
        pokeDouble(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DADD)
    public static void dadd() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        BytecodeCounter.inc(99);
        removeSlots(2);
        pokeDouble(0, value1 + value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DSUB)
    public static void dsub() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        BytecodeCounter.inc(103);
        removeSlots(2);
        pokeDouble(0, value1 - value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DMUL)
    public static void dmul() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        BytecodeCounter.inc(107);
        removeSlots(2);
        pokeDouble(0, value1 * value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DDIV)
    public static void ddiv() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        BytecodeCounter.inc(111);
        removeSlots(2);
        pokeDouble(0, value1 / value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DREM)
    public static void drem() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        BytecodeCounter.inc(115);
        removeSlots(2);
        pokeDouble(0, value1 % value2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DNEG)
    public static void dneg(double zero) {
        double value = zero - peekDouble(0);
        BytecodeCounter.inc(119);
        pokeDouble(0, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DCMPG)
    public static void dcmpgOp() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        int result = dcmpg(value1, value2);
        BytecodeCounter.inc(152);
        removeSlots(3);
        pokeInt(0, result);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DCMPL)
    public static void dcmplOp() {
        double value2 = peekDouble(0);
        double value1 = peekDouble(2);
        int result = dcmpl(value1, value2);
        BytecodeCounter.inc(151);
        removeSlots(3);
        pokeInt(0, result);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DRETURN)
    public static double dreturn() {
        BytecodeCounter.inc(175);
        return popDouble();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DRETURN$unlockClass)
    public static double dreturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(175);
        Monitor.noninlineExit(rcvr);
        return popDouble();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DRETURN$unlockReceiver)
    public static double dreturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(175);
        Monitor.noninlineExit(rcvr);
        return popDouble();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DALOAD)
    public static void daload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(49);
        pokeDouble(0, ArrayAccess.getDouble(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DASTORE)
    public static void dastore() {
        int index = peekInt(2);
        Object array = peekObject(3);
        ArrayAccess.checkIndex(array, index);
        double value = peekDouble(0);
        BytecodeCounter.inc(82);
        ArrayAccess.setDouble(array, index, value);
        removeSlots(4);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$double)
    public static void invokevirtualDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$double$resolved)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$double$instrumented)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$double)
    public static void invokeinterfaceDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$double$resolved)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$double$instrumented)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$double)
    public static void invokespecialDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        indirectCallDouble(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$double$resolved)
    public static void invokespecialDouble(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        directCallDouble();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$double)
    public static void invokestaticDouble(ResolutionGuard.InPool guard) {
        BytecodeCounter.inc(184);
        indirectCallDouble(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$double$init)
    public static void invokestaticDouble() {
        BytecodeCounter.inc(184);
        directCallDouble();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_DOUBLE)
    public static void pget_double() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(2017);
        removeSlots(1);
        pokeDouble(0, ptr.getDouble(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_DOUBLE)
    public static void pset_double() {
        double value = peekDouble(0);
        int index = peekInt(2);
        int disp = peekInt(3);
        Pointer ptr = peekWord(4).asPointer();
        BytecodeCounter.inc(1762);
        removeSlots(5);
        ptr.setDouble(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_DOUBLE)
    public static void pread_double() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(2015);
        pokeDouble(0, ptr.readDouble(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_DOUBLE)
    public static void pwrite_double() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        double value = peekDouble(0);
        BytecodeCounter.inc(1760);
        removeSlots(4);
        ptr.writeDouble(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_DOUBLE_I)
    public static void pread_double_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(4319);
        pokeDouble(0, ptr.readDouble(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_DOUBLE_I)
    public static void pwrite_double_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        double value = peekDouble(0);
        BytecodeCounter.inc(3808);
        removeSlots(4);
        ptr.writeDouble(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ACONST_NULL)
    public static void aconst_null() {
        BytecodeCounter.inc(1);
        pushObject(null);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$reference$resolved)
    public static void getfieldReference(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeObject(0, TupleAccess.readObject(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$reference)
    public static void getfieldReference(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeObject(0, resolveAndGetFieldReference(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Object resolveAndGetFieldReference(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readObject(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$reference)
    public static void getstaticReference(ResolutionGuard.InPool guard) {
        pushObject(resolveAndGetStaticReference(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Object resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readObject(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$reference$init)
    public static void getstaticReference(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushObject(TupleAccess.readObject(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$reference$resolved)
    public static void putfieldReference(int offset) {
        Object object = peekObject(1);
        Object value = peekObject(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        Object value = peekObject(0);
        resolveAndPutFieldReference(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Object value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$reference$init)
    public static void putstaticReference(Object staticTuple, int offset) {
        Object value = popObject();
        BytecodeCounter.inc(179);
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard) {
        resolveAndPutStaticReference(guard, popObject());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Object value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeObject(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ALOAD)
    public static void aload(int dispToLocalSlot) {
        Object value = getLocalObject(dispToLocalSlot);
        BytecodeCounter.inc(25);
        pushObject(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ASTORE)
    public static void astore(int dispToLocalSlot) {
        Object value = popObject();
        BytecodeCounter.inc(58);
        setLocalObject(dispToLocalSlot, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDC$reference$resolved)
    public static void rldc(Object constant) {
        BytecodeCounter.inc(18);
        pushObject(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LDC$reference)
    public static void urldc(ResolutionGuard guard) {
        ClassActor classActor = Snippets.resolveClass(guard);
        Object constant = T1XRuntime.getClassMirror(classActor);
        BytecodeCounter.inc(18);
        pushObject(constant);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ACMPEQ)
    public static void if_acmpeq() {
        BytecodeCounter.inc(165);
        acmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IF_ACMPNE)
    public static void if_acmpne() {
        BytecodeCounter.inc(166);
        acmp_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFNULL)
    public static void ifnull() {
        BytecodeCounter.inc(198);
        acmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IFNONNULL)
    public static void ifnonnull() {
        BytecodeCounter.inc(199);
        acmp0_prefix();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ARETURN)
    public static Object areturn() {
        BytecodeCounter.inc(176);
        return popObject();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ARETURN$unlockClass)
    public static Object areturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(176);
        Monitor.noninlineExit(rcvr);
        return popObject();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ARETURN$unlockReceiver)
    public static Object areturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(176);
        Monitor.noninlineExit(rcvr);
        return popObject();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(AALOAD)
    public static void aaload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        BytecodeCounter.inc(50);
        removeSlots(1);
        pokeObject(0, ArrayAccess.getObject(array, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(AASTORE)
    public static void aastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        Object value = peekObject(0);
        BytecodeCounter.inc(83);
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_REFERENCE)
    public static void pget_reference() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(2529);
        removeSlots(2);
        pokeReference(0, ptr.getReference(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_REFERENCE)
    public static void pset_reference() {
        Reference value = peekReference(0);
        int index = peekInt(1);
        int disp = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(2274);
        removeSlots(4);
        ptr.setReference(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_REFERENCE)
    public static void pread_reference() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(2527);
        removeSlots(1);
        pokeReference(0, ptr.readReference(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_REFERENCE)
    public static void pwrite_reference() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        Reference value = peekReference(0);
        BytecodeCounter.inc(2272);
        removeSlots(3);
        ptr.writeReference(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_REFERENCE_I)
    public static void pread_reference_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(4831);
        removeSlots(1);
        pokeReference(0, ptr.readReference(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_REFERENCE_I)
    public static void pwrite_reference_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        Reference value = peekReference(0);
        BytecodeCounter.inc(4320);
        removeSlots(3);
        ptr.writeReference(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PCMPSWP_REFERENCE)
    public static void pcmpswp_reference() {
        Reference newValue = peekReference(0);
        Reference expectedValue = peekReference(1);
        Offset off = peekWord(2).asOffset();
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(1251);
        removeSlots(3);
        pokeReference(0, ptr.compareAndSwapReference(off, expectedValue, newValue));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PCMPSWP_REFERENCE_I)
    public static void pcmpswp_reference_i() {
        Reference newValue = peekReference(0);
        Reference expectedValue = peekReference(1);
        int off = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(2275);
        removeSlots(3);
        pokeReference(0, ptr.compareAndSwapReference(off, expectedValue, newValue));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WCONST_0)
    public static void wconst_0() {
        BytecodeCounter.inc(216);
        pushWord(Address.zero());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$word$resolved)
    public static void getfieldWord(int offset) {
        Object object = peekObject(0);
        BytecodeCounter.inc(180);
        pokeWord(0, TupleAccess.readWord(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$word)
    public static void getfieldWord(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeWord(0, resolveAndGetFieldWord(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        BytecodeCounter.inc(180);
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readWord(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$word)
    public static void getstaticWord(ResolutionGuard.InPool guard) {
        pushWord(resolveAndGetStaticWord(guard));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        BytecodeCounter.inc(178);
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readWord(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$word$init)
    public static void getstaticWord(Object staticTuple, int offset) {
        BytecodeCounter.inc(178);
        pushWord(TupleAccess.readWord(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$word$resolved)
    public static void putfieldWord(int offset) {
        Object object = peekObject(1);
        Word value = peekWord(0);
        BytecodeCounter.inc(181);
        removeSlots(2);
        TupleAccess.writeWord(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        Word value = peekWord(0);
        resolveAndPutFieldWord(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        BytecodeCounter.inc(181);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$word$init)
    public static void putstaticWord(Object staticTuple, int offset) {
        Word value = popWord();
        BytecodeCounter.inc(179);
        TupleAccess.writeWord(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard) {
        resolveAndPutStaticWord(guard, popWord());
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        BytecodeCounter.inc(179);
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeWord(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WLOAD)
    public static void wload(int dispToLocalSlot) {
        Word value = getLocalWord(dispToLocalSlot);
        BytecodeCounter.inc(206);
        pushWord(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WSTORE)
    public static void wstore(int dispToLocalSlot) {
        Word value = popWord();
        BytecodeCounter.inc(211);
        setLocalWord(dispToLocalSlot, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WRETURN)
    public static Word wreturn() {
        BytecodeCounter.inc(238);
        return popWord();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WRETURN$unlockClass)
    public static Word wreturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(238);
        Monitor.noninlineExit(rcvr);
        return popWord();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WRETURN$unlockReceiver)
    public static Word wreturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(238);
        Monitor.noninlineExit(rcvr);
        return popWord();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$word)
    public static void invokevirtualWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$word$resolved)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$word$instrumented)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$word)
    public static void invokeinterfaceWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$word$resolved)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$word$instrumented)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$word)
    public static void invokespecialWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        indirectCallWord(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$word$resolved)
    public static void invokespecialWord(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        directCallWord();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$word)
    public static void invokestaticWord(ResolutionGuard.InPool guard) {
        BytecodeCounter.inc(184);
        indirectCallWord(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$word$init)
    public static void invokestaticWord() {
        BytecodeCounter.inc(184);
        directCallWord();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PGET_WORD)
    public static void pget_word() {
        int index = peekInt(0);
        int disp = peekInt(1);
        Pointer ptr = peekWord(2).asPointer();
        BytecodeCounter.inc(2273);
        removeSlots(2);
        pokeWord(0, ptr.getWord(disp, index));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PSET_WORD)
    public static void pset_word() {
        Word value = peekWord(0);
        int index = peekInt(1);
        int disp = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(2018);
        removeSlots(4);
        ptr.setWord(disp, index, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_WORD)
    public static void pread_word() {
        Offset off = peekWord(0).asOffset();
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(2271);
        removeSlots(1);
        pokeWord(0, ptr.readWord(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_WORD)
    public static void pwrite_word() {
        Pointer ptr = peekWord(2).asPointer();
        Offset off = peekWord(1).asOffset();
        Word value = peekWord(0);
        BytecodeCounter.inc(2016);
        removeSlots(3);
        ptr.writeWord(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PREAD_WORD_I)
    public static void pread_word_i() {
        int off = peekInt(0);
        Pointer ptr = peekWord(1).asPointer();
        BytecodeCounter.inc(4575);
        removeSlots(1);
        pokeWord(0, ptr.readWord(off));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PWRITE_WORD_I)
    public static void pwrite_word_i() {
        Pointer ptr = peekWord(2).asPointer();
        int off = peekInt(1);
        Word value = peekWord(0);
        BytecodeCounter.inc(4064);
        removeSlots(3);
        ptr.writeWord(off, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PCMPSWP_WORD)
    public static void pcmpswp_word() {
        Word newValue = peekWord(0);
        Word expectedValue = peekWord(1);
        Offset off = peekWord(2).asOffset();
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(995);
        removeSlots(3);
        pokeWord(0, ptr.compareAndSwapWord(off, expectedValue, newValue));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PCMPSWP_WORD_I)
    public static void pcmpswp_word_i() {
        Word newValue = peekWord(0);
        Word expectedValue = peekWord(1);
        int off = peekInt(2);
        Pointer ptr = peekWord(3).asPointer();
        BytecodeCounter.inc(2019);
        removeSlots(3);
        pokeWord(0, ptr.compareAndSwapWord(off, expectedValue, newValue));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(RETURN)
    public static void vreturn() {
        BytecodeCounter.inc(177);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(RETURN$unlockClass)
    public static void vreturnUnlockClass(Class<?> rcvr) {
        BytecodeCounter.inc(177);
        Monitor.noninlineExit(rcvr);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(RETURN$unlockReceiver)
    public static void vreturnUnlockReceiver(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(177);
        Monitor.noninlineExit(rcvr);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$void)
    public static void invokevirtualVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$void$resolved)
    public static void invokevirtualVoid(int vTableIndex, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEVIRTUAL$void$instrumented)
    public static void invokevirtualVoid(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(182);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$void)
    public static void invokeinterfaceVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$void$resolved)
    public static void invokeinterfaceVoid(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKEINTERFACE$void$instrumented)
    public static void invokeinterfaceVoid(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = peekObject(receiverStackIndex);
        BytecodeCounter.inc(185);
        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$void)
    public static void invokespecialVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        indirectCallVoid(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$void$resolved)
    public static void invokespecialVoid(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        BytecodeCounter.inc(183);
        directCallVoid();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$void)
    public static void invokestaticVoid(ResolutionGuard.InPool guard) {
        BytecodeCounter.inc(184);
        indirectCallVoid(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESTATIC$void$init)
    public static void invokestaticVoid() {
        BytecodeCounter.inc(184);
        directCallVoid();
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WDIV)
    public static void wdiv() {
        Address value2 = peekWord(0).asAddress();
        Address value1 = peekWord(1).asAddress();
        BytecodeCounter.inc(217);
        removeSlots(1);
        pokeWord(0, value1.dividedBy(value2));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WDIVI)
    public static void wdivi() {
        int value2 = peekInt(0);
        Address value1 = peekWord(1).asAddress();
        BytecodeCounter.inc(218);
        removeSlots(1);
        pokeWord(0, value1.dividedBy(value2));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WREM)
    public static void wrem() {
        Address value2 = peekWord(0).asAddress();
        Address value1 = peekWord(1).asAddress();
        BytecodeCounter.inc(219);
        removeSlots(1);
        pokeWord(0, value1.remainder(value2));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(WREMI)
    public static void wremi() {
        int value2 = peekInt(0);
        Address value1 = peekWord(1).asAddress();
        BytecodeCounter.inc(220);
        removeSlots(1);
        pokeInt(0, value1.remainder(value2));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IINC)
    public static void iinc(int dispToLocalSlot, int increment) {
        int value = getLocalInt(dispToLocalSlot);
        BytecodeCounter.inc(132);
        setLocalInt(dispToLocalSlot, value  + increment);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MOV_F2I)
    public static void mov_f2i() {
        float value = peekFloat(0);
        BytecodeCounter.inc(229);
        pokeInt(0, Intrinsics.floatToInt(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MOV_I2F)
    public static void mov_i2f() {
        int value = peekInt(0);
        BytecodeCounter.inc(228);
        pokeFloat(0, Intrinsics.intToFloat(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MOV_D2L)
    public static void mov_d2l() {
        double value = peekDouble(0);
        BytecodeCounter.inc(231);
        pokeLong(0, Intrinsics.doubleToLong(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MOV_L2D)
    public static void mov_l2d() {
        long value = peekLong(0);
        BytecodeCounter.inc(230);
        pokeDouble(0, Intrinsics.longToDouble(value));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(BIPUSH)
    public static void bipush(byte value) {
        BytecodeCounter.inc(16);
        pushInt(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(SIPUSH)
    public static void sipush(short value) {
        BytecodeCounter.inc(17);
        pushInt(value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(POP)
    public static void pop() {
        BytecodeCounter.inc(87);
        removeSlots(1);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(POP2)
    public static void pop2() {
        BytecodeCounter.inc(88);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEW)
    public static void new_(ResolutionGuard arg) {
        Object object = resolveClassForNewAndCreate(arg);
        pushObject(object);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEW$init)
    public static void new_(ClassActor arg) {
        Object object = createTupleOrHybrid(arg);
        pushObject(object);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEWARRAY)
    public static void newarray(Kind<?> kind) {
        int length = peekInt(0);
        Object array = createPrimitiveArray(kind, length);
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ANEWARRAY)
    public static void anewarray(ResolutionGuard guard) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(guard));
        int length = peekInt(0);
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ANEWARRAY$resolved)
    public static void anewarray(ArrayClassActor<?> arrayClassActor) {
        int length = peekInt(0);
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MULTIANEWARRAY)
    public static void multianewarray(ResolutionGuard guard, int[] lengthsShared) {
        ClassActor arrayClassActor = Snippets.resolveClass(guard);
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that
        // causes this template to have a reference literal in its compiled form.
        int[] lengths = UnsafeCast.asIntArray(cloneArray(lengthsShared));
        int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            int length = popInt();
            checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }

        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        pushObject(array);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MULTIANEWARRAY$resolved)
    public static void multianewarray(ArrayClassActor<?> arrayClassActor, int[] lengthsShared) {
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that
        // causes this template to have a reference literal in its compiled form.
        int[] lengths = UnsafeCast.asIntArray(cloneArray(lengthsShared));
        int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            int length = popInt();
            checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }

        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);
        pushObject(array);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(CHECKCAST)
    public static void checkcast(ResolutionGuard arg) {
        Object value = peekObject(0);
        BytecodeCounter.inc(192);
        resolveAndCheckcast(arg, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(CHECKCAST$resolved)
    public static void checkcast(ClassActor arg) {
        Object value = peekObject(0);
        BytecodeCounter.inc(192);
        Snippets.checkCast(arg, value);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ARRAYLENGTH)
    public static void arraylength() {
        Object array = peekObject(0);
        int length = ArrayAccess.readArrayLength(array);
        BytecodeCounter.inc(190);
        pokeInt(0, length);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ATHROW)
    public static void athrow() {
        Object object = peekObject(0);
        BytecodeCounter.inc(191);
        Throw.raise(object);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MONITORENTER)
    public static void monitorenter() {
        Object object = peekObject(0);
        BytecodeCounter.inc(194);
        T1XRuntime.monitorenter(object);
        removeSlots(1);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(MONITOREXIT)
    public static void monitorexit() {
        Object object = peekObject(0);
        BytecodeCounter.inc(195);
        T1XRuntime.monitorexit(object);
        removeSlots(1);
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INSTANCEOF)
    public static void instanceof_(ResolutionGuard guard) {
        ClassActor classActor = Snippets.resolveClass(guard);
        Object object = peekObject(0);
        BytecodeCounter.inc(193);
        pokeInt(0, UnsafeCast.asByte(Snippets.instanceOf(classActor, object)));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INSTANCEOF$resolved)
    public static void instanceof_(ClassActor classActor) {
        Object object = peekObject(0);
        BytecodeCounter.inc(193);
        pokeInt(0, UnsafeCast.asByte(Snippets.instanceOf(classActor, object)));
    }

    // GENERATED -- EDIT AND RUN VMACountBytecodesTemplateSourceGenerator.main() TO MODIFY
    @T1X_TEMPLATE(RETURN$registerFinalizer)
    public static void vreturnRegisterFinalizer(int dispToRcvrCopy) {
        Object rcvr = getLocalObject(dispToRcvrCopy);
        BytecodeCounter.inc(177);
        if (ObjectAccess.readClassActor(rcvr).hasFinalizer()) {
            SpecialReferenceManager.registerFinalizee(rcvr);
        }
    }


}
