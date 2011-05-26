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

import static com.sun.max.vm.t1x.T1XFrameOps.*;
import static com.sun.max.vm.t1x.T1XRuntime.*;
import static com.sun.max.vm.t1x.T1XTemplateTag.*;
import static com.sun.max.vm.t1x.T1XTemplateSource.*;
import static com.oracle.max.vm.ext.vma.run.java.VMAJavaRunScheme.*;

import com.oracle.max.vm.ext.vma.runtime.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.t1x.T1XRuntime;
import com.sun.max.vm.t1x.T1X_TEMPLATE;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.type.*;

public class VMAAdviceTemplateSource {

    @INLINE
    private static boolean isAdvising() {
        return VmThread.currentTLA().getWord(VM_ADVISING.index) != Word.zero();
    }

    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEW)
    public static void new_(ResolutionGuard arg) {
        Object object = resolveClassForNewAndCreate(arg);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNew(object);
        }
        pushObject(object);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEW$init)
    public static void new_(ClassActor arg) {
        Object object = createTupleOrHybrid(arg);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNew(object);
        }
        pushObject(object);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(NEWARRAY)
    public static void newarray(Kind<?> kind) {
        int length = peekInt(0);
        Object array = createPrimitiveArray(kind, length);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ANEWARRAY)
    public static void anewarray(ResolutionGuard guard) {
        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(guard));
        int length = peekInt(0);
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(ANEWARRAY$resolved)
    public static void anewarray(ArrayClassActor<?> arrayClassActor) {
        int length = peekInt(0);
        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterNewArray(array, length);
        }
        pokeObject(0, array);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
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
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(array, lengths);
        }
        pushObject(array);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
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
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterMultiNewArray(array, lengths);
        }
        pushObject(array);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$void)
    public static void invokespecialVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        indirectCallVoid(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(INVOKESPECIAL$void$resolved)
    public static void invokespecialVoid(int receiverStackIndex) {
        Pointer receiver = peekWord(receiverStackIndex).asPointer();
        nullCheck(receiver);
        directCallVoid();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseAfterInvokeSpecial(Reference.fromOrigin(receiver).toJava());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(int offset) {
        Object object = peekObject(1);
        boolean value = peekBoolean(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value ? 1 : 0);
        }
        removeSlots(2);
        TupleAccess.writeBoolean(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        boolean value = peekBoolean(0);
        resolveAndPutFieldBoolean(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldBoolean(ResolutionGuard.InPool guard, Object object, boolean value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value ? 1 : 0);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$byte$resolved)
    public static void putfieldByte(int offset) {
        Object object = peekObject(1);
        byte value = peekByte(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(2);
        TupleAccess.writeByte(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        byte value = peekByte(0);
        resolveAndPutFieldByte(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldByte(ResolutionGuard.InPool guard, Object object, byte value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$char$resolved)
    public static void putfieldChar(int offset) {
        Object object = peekObject(1);
        char value = peekChar(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(2);
        TupleAccess.writeChar(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        char value = peekChar(0);
        resolveAndPutFieldChar(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldChar(ResolutionGuard.InPool guard, Object object, char value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$short$resolved)
    public static void putfieldShort(int offset) {
        Object object = peekObject(1);
        short value = peekShort(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(2);
        TupleAccess.writeShort(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        short value = peekShort(0);
        resolveAndPutFieldShort(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldShort(ResolutionGuard.InPool guard, Object object, short value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, f.offset(), value);
        }
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(object, f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(object, f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$int$resolved)
    public static void putfieldInt(int offset) {
        Object object = peekObject(1);
        int value = peekInt(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(2);
        TupleAccess.writeInt(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        int value = peekInt(0);
        resolveAndPutFieldInt(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldInt(ResolutionGuard.InPool guard, Object object, int value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$float$resolved)
    public static void putfieldFloat(int offset) {
        Object object = peekObject(1);
        float value = peekFloat(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(2);
        TupleAccess.writeFloat(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        float value = peekFloat(0);
        resolveAndPutFieldFloat(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldFloat(ResolutionGuard.InPool guard, Object object, float value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$long$resolved)
    public static void putfieldLong(int offset) {
        Object object = peekObject(2);
        long value = peekLong(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(3);
        TupleAccess.writeLong(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard) {
        Object object = peekObject(2);
        long value = peekLong(0);
        resolveAndPutFieldLong(guard, object, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldLong(ResolutionGuard.InPool guard, Object object, long value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$double$resolved)
    public static void putfieldDouble(int offset) {
        Object object = peekObject(2);
        double value = peekDouble(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(3);
        TupleAccess.writeDouble(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard) {
        Object object = peekObject(2);
        double value = peekDouble(0);
        resolveAndPutFieldDouble(guard, object, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldDouble(ResolutionGuard.InPool guard, Object object, double value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$reference$resolved)
    public static void putfieldReference(int offset) {
        Object object = peekObject(1);
        Object value = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value);
        }
        removeSlots(2);
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        Object value = peekObject(0);
        resolveAndPutFieldReference(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldReference(ResolutionGuard.InPool guard, Object object, Object value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$word$resolved)
    public static void putfieldWord(int offset) {
        Object object = peekObject(1);
        Word value = peekWord(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutField(object, offset, value.asAddress().toLong());
        }
        removeSlots(2);
        TupleAccess.writeWord(object, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard) {
        Object object = peekObject(1);
        Word value = peekWord(0);
        resolveAndPutFieldWord(guard, object, value);
        removeSlots(2);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutFieldWord(ResolutionGuard.InPool guard, Object object, Word value) {
        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$boolean$init)
    public static void putstaticBoolean(Object staticTuple, int offset) {
        boolean value = popBoolean();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value ? 1 : 0);
        }
        TupleAccess.writeBoolean(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard) {
        resolveAndPutStaticBoolean(guard, popBoolean());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticBoolean(ResolutionGuard.InPool guard, boolean value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value ? 1 : 0);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeBoolean(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$byte$init)
    public static void putstaticByte(Object staticTuple, int offset) {
        byte value = popByte();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeByte(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard) {
        resolveAndPutStaticByte(guard, popByte());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticByte(ResolutionGuard.InPool guard, byte value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeByte(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$char$init)
    public static void putstaticChar(Object staticTuple, int offset) {
        char value = popChar();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeChar(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard) {
        resolveAndPutStaticChar(guard, popChar());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticChar(ResolutionGuard.InPool guard, char value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeChar(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$short$init)
    public static void putstaticShort(Object staticTuple, int offset) {
        short value = popShort();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeShort(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard) {
        resolveAndPutStaticShort(guard, popShort());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticShort(ResolutionGuard.InPool guard, short value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(f.holder().staticTuple(), f.offset(), value);
        }
        Snippets.makeHolderInitialized(f);
        if (f.isVolatile()) {
            preVolatileWrite();
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), value);
            postVolatileWrite();
        } else {
            TupleAccess.writeShort(f.holder().staticTuple(), f.offset(), value);
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$int$init)
    public static void putstaticInt(Object staticTuple, int offset) {
        int value = popInt();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard) {
        resolveAndPutStaticInt(guard, popInt());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticInt(ResolutionGuard.InPool guard, int value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$float$init)
    public static void putstaticFloat(Object staticTuple, int offset) {
        float value = popFloat();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard) {
        resolveAndPutStaticFloat(guard, popFloat());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticFloat(ResolutionGuard.InPool guard, float value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$long$init)
    public static void putstaticLong(Object staticTuple, int offset) {
        long value = popLong();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard) {
        resolveAndPutStaticLong(guard, popLong());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticLong(ResolutionGuard.InPool guard, long value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$double$init)
    public static void putstaticDouble(Object staticTuple, int offset) {
        double value = popDouble();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard) {
        resolveAndPutStaticDouble(guard, popDouble());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticDouble(ResolutionGuard.InPool guard, double value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$reference$init)
    public static void putstaticReference(Object staticTuple, int offset) {
        Object value = popObject();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value);
        }
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard) {
        resolveAndPutStaticReference(guard, popObject());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticReference(ResolutionGuard.InPool guard, Object value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$word$init)
    public static void putstaticWord(Object staticTuple, int offset) {
        Word value = popWord();
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforePutStatic(staticTuple, offset, value.asAddress().toLong());
        }
        TupleAccess.writeWord(staticTuple, offset, value);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard) {
        resolveAndPutStaticWord(guard, popWord());
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static void resolveAndPutStaticWord(ResolutionGuard.InPool guard, Word value) {
        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);
        if (isAdvising()) {
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

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$boolean$resolved)
    public static void getfieldBoolean(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        pokeBoolean(0, TupleAccess.readBoolean(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$boolean)
    public static void getfieldBoolean(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeBoolean(0, resolveAndGetFieldBoolean(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static boolean resolveAndGetFieldBoolean(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readBoolean(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$byte$resolved)
    public static void getfieldByte(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        pokeByte(0, TupleAccess.readByte(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$byte)
    public static void getfieldByte(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeByte(0, resolveAndGetFieldByte(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static byte resolveAndGetFieldByte(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readByte(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$char$resolved)
    public static void getfieldChar(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        pokeChar(0, TupleAccess.readChar(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$char)
    public static void getfieldChar(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeChar(0, resolveAndGetFieldChar(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static char resolveAndGetFieldChar(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readChar(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$short$resolved)
    public static void getfieldShort(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        pokeShort(0, TupleAccess.readShort(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$short)
    public static void getfieldShort(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeShort(0, resolveAndGetFieldShort(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static short resolveAndGetFieldShort(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readShort(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$int$resolved)
    public static void getfieldInt(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        pokeInt(0, TupleAccess.readInt(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$int)
    public static void getfieldInt(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeInt(0, resolveAndGetFieldInt(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static int resolveAndGetFieldInt(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readInt(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$float$resolved)
    public static void getfieldFloat(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0.0f);
        }
        pokeFloat(0, TupleAccess.readFloat(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$float)
    public static void getfieldFloat(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeFloat(0, resolveAndGetFieldFloat(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static float resolveAndGetFieldFloat(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0.0f);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readFloat(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$long$resolved)
    public static void getfieldLong(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        addSlots(1);
        pokeLong(0, TupleAccess.readLong(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$long)
    public static void getfieldLong(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        addSlots(1);
        pokeLong(0, resolveAndGetFieldLong(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static long resolveAndGetFieldLong(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readLong(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$double$resolved)
    public static void getfieldDouble(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0.0);
        }
        addSlots(1);
        pokeDouble(0, TupleAccess.readDouble(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$double)
    public static void getfieldDouble(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        addSlots(1);
        pokeDouble(0, resolveAndGetFieldDouble(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static double resolveAndGetFieldDouble(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0.0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readDouble(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$reference$resolved)
    public static void getfieldReference(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, null);
        }
        pokeObject(0, TupleAccess.readObject(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$reference)
    public static void getfieldReference(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeObject(0, resolveAndGetFieldReference(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Object resolveAndGetFieldReference(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), null);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readObject(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$word$resolved)
    public static void getfieldWord(int offset) {
        Object object = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, offset, 0);
        }
        pokeWord(0, TupleAccess.readWord(object, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETFIELD$word)
    public static void getfieldWord(ResolutionGuard.InPool guard) {
        Object object = peekObject(0);
        pokeWord(0, resolveAndGetFieldWord(guard, object));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Word resolveAndGetFieldWord(ResolutionGuard.InPool guard, Object object) {
        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetField(object, f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(object, f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readWord(object, f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$boolean)
    public static void getstaticBoolean(ResolutionGuard.InPool guard) {
        pushBoolean(resolveAndGetStaticBoolean(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static boolean resolveAndGetStaticBoolean(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            boolean value = TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readBoolean(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$boolean$init)
    public static void getstaticBoolean(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushBoolean(TupleAccess.readBoolean(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$byte)
    public static void getstaticByte(ResolutionGuard.InPool guard) {
        pushByte(resolveAndGetStaticByte(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static byte resolveAndGetStaticByte(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            byte value = TupleAccess.readByte(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readByte(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$byte$init)
    public static void getstaticByte(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushByte(TupleAccess.readByte(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$char)
    public static void getstaticChar(ResolutionGuard.InPool guard) {
        pushChar(resolveAndGetStaticChar(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static char resolveAndGetStaticChar(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            char value = TupleAccess.readChar(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readChar(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$char$init)
    public static void getstaticChar(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushChar(TupleAccess.readChar(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$short)
    public static void getstaticShort(ResolutionGuard.InPool guard) {
        pushShort(resolveAndGetStaticShort(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static short resolveAndGetStaticShort(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            short value = TupleAccess.readShort(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readShort(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$short$init)
    public static void getstaticShort(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushShort(TupleAccess.readShort(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$int)
    public static void getstaticInt(ResolutionGuard.InPool guard) {
        pushInt(resolveAndGetStaticInt(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static int resolveAndGetStaticInt(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            int value = TupleAccess.readInt(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readInt(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$int$init)
    public static void getstaticInt(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushInt(TupleAccess.readInt(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$float)
    public static void getstaticFloat(ResolutionGuard.InPool guard) {
        pushFloat(resolveAndGetStaticFloat(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static float resolveAndGetStaticFloat(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0.0f);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            float value = TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readFloat(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$float$init)
    public static void getstaticFloat(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0.0f);
        }
        pushFloat(TupleAccess.readFloat(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$long)
    public static void getstaticLong(ResolutionGuard.InPool guard) {
        pushLong(resolveAndGetStaticLong(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static long resolveAndGetStaticLong(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            long value = TupleAccess.readLong(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readLong(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$long$init)
    public static void getstaticLong(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushLong(TupleAccess.readLong(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$double)
    public static void getstaticDouble(ResolutionGuard.InPool guard) {
        pushDouble(resolveAndGetStaticDouble(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static double resolveAndGetStaticDouble(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0.0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            double value = TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readDouble(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$double$init)
    public static void getstaticDouble(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0.0);
        }
        pushDouble(TupleAccess.readDouble(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$reference)
    public static void getstaticReference(ResolutionGuard.InPool guard) {
        pushObject(resolveAndGetStaticReference(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Object resolveAndGetStaticReference(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), null);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Object value = TupleAccess.readObject(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readObject(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$reference$init)
    public static void getstaticReference(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, null);
        }
        pushObject(TupleAccess.readObject(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$word)
    public static void getstaticWord(ResolutionGuard.InPool guard) {
        pushWord(resolveAndGetStaticWord(guard));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @NEVER_INLINE
    public static Word resolveAndGetStaticWord(ResolutionGuard.InPool guard) {
        FieldActor f = Snippets.resolveStaticFieldForReading(guard);
        Snippets.makeHolderInitialized(f);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(f.holder().staticTuple(), f.offset(), 0);
        }
        if (f.isVolatile()) {
            preVolatileRead();
            Word value = TupleAccess.readWord(f.holder().staticTuple(), f.offset());
            postVolatileRead();
            return value;
        } else {
            return TupleAccess.readWord(f.holder().staticTuple(), f.offset());
        }
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(GETSTATIC$word$init)
    public static void getstaticWord(Object staticTuple, int offset) {
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeGetStatic(staticTuple, offset, 0);
        }
        pushWord(TupleAccess.readWord(staticTuple, offset));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(BALOAD)
    public static void baload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0);
        }
        removeSlots(1);
        pokeBoolean(0, ArrayAccess.getBoolean(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(CALOAD)
    public static void caload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0);
        }
        removeSlots(1);
        pokeChar(0, ArrayAccess.getChar(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(SALOAD)
    public static void saload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0);
        }
        removeSlots(1);
        pokeShort(0, ArrayAccess.getShort(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IALOAD)
    public static void iaload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0);
        }
        removeSlots(1);
        pokeInt(0, ArrayAccess.getInt(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FALOAD)
    public static void faload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0.0f);
        }
        removeSlots(1);
        pokeFloat(0, ArrayAccess.getFloat(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LALOAD)
    public static void laload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0);
        }
        pokeLong(0, ArrayAccess.getLong(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DALOAD)
    public static void daload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, 0.0);
        }
        pokeDouble(0, ArrayAccess.getDouble(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(AALOAD)
    public static void aaload() {
        int index = peekInt(0);
        Object array = peekObject(1);
        ArrayAccess.checkIndex(array, index);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayLoad(array, index, null);
        }
        removeSlots(1);
        pokeObject(0, ArrayAccess.getObject(array, index));
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(BASTORE)
    public static void bastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        boolean value = peekBoolean(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value ? 1 : 0);
        }
        ArrayAccess.setBoolean(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(CASTORE)
    public static void castore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        char value = peekChar(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setChar(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(SASTORE)
    public static void sastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        short value = peekShort(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setShort(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(IASTORE)
    public static void iastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        int value = peekInt(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setInt(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(FASTORE)
    public static void fastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        float value = peekFloat(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setFloat(array, index, value);
        removeSlots(3);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(LASTORE)
    public static void lastore() {
        int index = peekInt(2);
        Object array = peekObject(3);
        ArrayAccess.checkIndex(array, index);
        long value = peekLong(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setLong(array, index, value);
        removeSlots(4);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(DASTORE)
    public static void dastore() {
        int index = peekInt(2);
        Object array = peekObject(3);
        ArrayAccess.checkIndex(array, index);
        double value = peekDouble(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.setDouble(array, index, value);
        removeSlots(4);
    }

    // GENERATED -- EDIT AND RUN VMAAdviceTemplateGenerator.main() TO MODIFY
    @T1X_TEMPLATE(AASTORE)
    public static void aastore() {
        int index = peekInt(1);
        Object array = peekObject(2);
        ArrayAccess.checkIndex(array, index);
        Object value = peekObject(0);
        if (isAdvising()) {
            VMAStaticBytecodeAdvice.adviseBeforeArrayStore(array, index, value);
        }
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
        removeSlots(3);
    }




}
