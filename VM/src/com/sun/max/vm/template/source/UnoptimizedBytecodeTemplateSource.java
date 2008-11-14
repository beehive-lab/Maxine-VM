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
package com.sun.max.vm.template.source;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NonFoldableSnippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * Class holding Java code the templates will be compiled from. There may be several sources for some bytecodes, such
 * that one source implement a bytecode based on a given set of assumption, i.e., resolved symbolic reference,
 * initialized class of operands, top of stack cached in register(s), and so on.
 *
 * This source of templates makes no assumptions.
 *
 * @author Laurent Daynes
 */
@TEMPLATE()
public final class UnoptimizedBytecodeTemplateSource {

    @INLINE
    public static void aconst_null() {
        JitStackFrameOperation.pushReference(null);
    }

    @INLINE
    public static void aaload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeReference(0, ArrayAccess.getObject(array, index));
    }

    @INLINE
    public static void aastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.noninlineCheckIndex(array, index);
        final Object value = JitStackFrameOperation.peekReference(0);
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.noinlineSetObject(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void aload(int dispToLocalSlot) {
        final Object value = JitStackFrameOperation.getLocalReference(dispToLocalSlot);
        JitStackFrameOperation.pushReference(value);
    }

    @INLINE
    public static void aload_0(int dispToLocalSlot) {
        aload(dispToLocalSlot);
    }

    @INLINE
    public static void aload_1(int dispToLocalSlot) {
        aload(dispToLocalSlot);
    }

    @INLINE
    public static void aload_2(int dispToLocalSlot) {
        aload(dispToLocalSlot);
    }

    @INLINE
    public static void aload_3(int dispToLocalSlot) {
        aload(dispToLocalSlot);
    }

    @INLINE
    public static void anewarray(ReferenceResolutionGuard guard) {
        final ArrayClassActor arrayClassActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveArrayClass.resolveArrayClass(guard));
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, NonFoldableSnippet.CreateReferenceArray.createReferenceArray(arrayClassActor, length));
    }

    @INLINE
    public static Object areturn() {
        return JitStackFrameOperation.popReference();
    }

    @INLINE
    public static void arraylength() {
        final int length = ArrayAccess.readArrayLength(JitStackFrameOperation.peekReference(0));
        JitStackFrameOperation.pokeInt(0, length);
    }

    @INLINE
    public static void astore(int displacementToSlot) {
        JitStackFrameOperation.setLocalReference(displacementToSlot, JitStackFrameOperation.popReference());
    }

    @INLINE
    public static void astore_0(int displacementToSlot) {
        astore(displacementToSlot);
    }

    @INLINE
    public static void astore_1(int displacementToSlot) {
        astore(displacementToSlot);
    }

    @INLINE
    public static void astore_2(int displacementToSlot) {
        astore(displacementToSlot);
    }

    @INLINE
    public static void astore_3(int displacementToSlot) {
        astore(displacementToSlot);
    }

    @INLINE
    public static void athrow() {
        Throw.raise(JitStackFrameOperation.popReference());
    }

    @INLINE
    public static void baload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getByte(array, index));
    }

    @INLINE
    public static void bastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.noninlineCheckIndex(array, index);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setByte(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void bipush(byte value) {
        JitStackFrameOperation.pushInt(value);
    }

    @INLINE
    public static void caload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getChar(array, index));
    }

    @INLINE
    public static void castore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.noninlineCheckIndex(array, index);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setChar(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void checkcast(ReferenceResolutionGuard guard) {
        final ClassActor classActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveClass.resolveClass(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        Snippet.CheckCast.checkCast(classActor, object);
    }

    @INLINE
    public static void d2f() {
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, (float) value);
    }

    @INLINE
    public static void d2i() {
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @INLINE
    public static void d2l() {
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.pokeLong(0, (long) value);
    }

    @INLINE
    public static void dadd() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 + value2);
    }

    @INLINE
    public static void daload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.pokeDouble(0, ArrayAccess.getDouble(array, index));
    }

    @INLINE
    public static void dastore() {
        final int index = JitStackFrameOperation.peekInt(2);
        final Object array = JitStackFrameOperation.peekReference(3);
        ArrayAccess.noninlineCheckIndex(array, index);
        final double value = JitStackFrameOperation.peekDouble(0);
        ArrayAccess.setDouble(array, index, value);
        JitStackFrameOperation.removeSlots(4);
    }

    @INLINE
    public static void dcmpg() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, JavaBuiltin.DoubleCompareG.doubleCompareG(value1, value2));
    }

    @INLINE
    public static void dcmpl() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, JavaBuiltin.DoubleCompareL.doubleCompareL(value1, value2));
    }

    @INLINE
    public static void dconst_0(double zero) {
        JitStackFrameOperation.pushDouble(zero);
    }

    @INLINE
    public static void dconst_1(double one) {
        JitStackFrameOperation.pushDouble(one);
    }

    @INLINE
    public static void ddiv() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 / value2);
    }

    @INLINE
    public static void dload(int displacementToSlot) {
        JitStackFrameOperation.pushDouble(JitStackFrameOperation.getLocalDouble(displacementToSlot));
    }

    @INLINE
    public static void dload_0(int displacementToSlot) {
        dload(displacementToSlot);
    }

    @INLINE
    public static void dload_1(int displacementToSlot) {
        dload(displacementToSlot);
    }

    @INLINE
    public static void dload_2(int displacementToSlot) {
        dload(displacementToSlot);
    }

    @INLINE
    public static void dload_3(int displacementToSlot) {
        dload(displacementToSlot);
    }

    @INLINE
    public static void dmul() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 * value2);
    }

    @INLINE
    public static void dneg(double zero) {
        JitStackFrameOperation.pokeDouble(0, zero - JitStackFrameOperation.peekDouble(0));
    }

    @INLINE
    public static void drem() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 % value2);
    }

    @INLINE
    public static double dreturn() {
        return JitStackFrameOperation.popDouble();
    }

    @INLINE
    public static void dstore(int displacementToSlot) {
        JitStackFrameOperation.setLocalDouble(displacementToSlot, JitStackFrameOperation.popDouble());
    }

    @INLINE
    public static void dstore_0(int displacementToSlot) {
        dstore(displacementToSlot);
    }

    @INLINE
    public static void dstore_1(int displacementToSlot) {
        dstore(displacementToSlot);
    }

    @INLINE
    public static void dstore_2(int displacementToSlot) {
        dstore(displacementToSlot);
    }

    @INLINE
    public static void dstore_3(int displacementToSlot) {
        dstore(displacementToSlot);
    }

    @INLINE
    public static void dsub() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 - value2);
    }

    @INLINE
    public static void dup() {
        JitStackFrameOperation.pushWord(JitStackFrameOperation.peekWord(0));
    }

    @INLINE
    public static void dup_x1() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pokeWord(1, value1);
        JitStackFrameOperation.pokeWord(0, value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @INLINE
    public static void dup_x2() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        final Word value3 = JitStackFrameOperation.peekWord(2);
        JitStackFrameOperation.pushWord(value1);
        JitStackFrameOperation.pokeWord(1, value2);
        JitStackFrameOperation.pokeWord(2, value3);
        JitStackFrameOperation.pokeWord(3, value1);
    }

    @INLINE
    public static void dup2() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @INLINE
    public static void dup2_x1() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        final Word value3 = JitStackFrameOperation.peekWord(2);
        JitStackFrameOperation.pokeWord(2, value2);
        JitStackFrameOperation.pokeWord(1, value1);
        JitStackFrameOperation.pokeWord(0, value3);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @INLINE
    public static void dup2_x2() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        final Word value3 = JitStackFrameOperation.peekWord(2);
        final Word value4 = JitStackFrameOperation.peekWord(3);
        JitStackFrameOperation.pokeWord(3, value2);
        JitStackFrameOperation.pokeWord(2, value1);
        JitStackFrameOperation.pokeWord(1, value4);
        JitStackFrameOperation.pokeWord(0, value3);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @INLINE
    public static void f2d() {
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @INLINE
    public static void f2i() {
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @INLINE
    public static void f2l() {
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, (long) value);
    }

    @INLINE
    public static void fadd() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 + value2);
    }

    @INLINE
    public static void faload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, ArrayAccess.getFloat(array, index));
    }

    @INLINE
    public static void fastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.noninlineCheckIndex(array, index);
        final float value = JitStackFrameOperation.peekFloat(0);
        ArrayAccess.setFloat(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void fcmpg() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        final int result = JavaBuiltin.FloatCompareG.floatCompareG(value1, value2);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @INLINE
    public static void fcmpl() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        final int result = JavaBuiltin.FloatCompareL.floatCompareL(value1, value2);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @INLINE
    public static void fconst_0(float zero) {
        JitStackFrameOperation.pushFloat(zero);
    }

    @INLINE
    public static void fconst_1(float one) {
        JitStackFrameOperation.pushFloat(one);
    }

    @INLINE
    public static void fconst_2(float two) {
        JitStackFrameOperation.pushFloat(two);
    }

    @INLINE
    public static void fdiv() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 / value2);
    }

    @INLINE
    public static void fload(int dispToLocalSlot) {
        final float value = JitStackFrameOperation.getLocalFloat(dispToLocalSlot);
        JitStackFrameOperation.pushFloat(value);
    }

    @INLINE
    public static void fload_0(int dispToLocalSlot) {
        fload(dispToLocalSlot);
    }

    @INLINE
    public static void fload_1(int dispToLocalSlot) {
        fload(dispToLocalSlot);
    }

    @INLINE
    public static void fload_2(int dispToLocalSlot) {
        fload(dispToLocalSlot);
    }

    @INLINE
    public static void fload_3(int dispToLocalSlot) {
        fload(dispToLocalSlot);
    }

    @INLINE
    public static void fmul() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 * value2);
    }

    @INLINE
    public static void fneg(float zero) {
        JitStackFrameOperation.pokeFloat(0, zero - JitStackFrameOperation.peekFloat(0));
    }

    @INLINE
    public static void frem() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 % value2);
    }

    @INLINE
    public static float freturn() {
        return JitStackFrameOperation.popFloat();
    }

    @INLINE
    public static void fstore(int displacementToSlot) {
        JitStackFrameOperation.setLocalFloat(displacementToSlot, JitStackFrameOperation.popFloat());
    }

    @INLINE
    public static void fstore_0(int displacementToSlot) {
        fstore(displacementToSlot);
    }

    @INLINE
    public static void fstore_1(int displacementToSlot) {
        fstore(displacementToSlot);
    }

    @INLINE
    public static void fstore_2(int displacementToSlot) {
        fstore(displacementToSlot);
    }

    @INLINE
    public static void fstore_3(int displacementToSlot) {
        fstore(displacementToSlot);
    }

    @INLINE
    public static void fsub() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 - value2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.REFERENCE)
    public static void rgetfield(ReferenceResolutionGuard guard) {
        final ReferenceFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeReference(0, FieldReadSnippet.ReadReference.readReference(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.WORD)
    public static void wgetfield(ReferenceResolutionGuard guard) {
        final WordFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeWord(0, FieldReadSnippet.ReadWord.readWord(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BYTE)
    public static void bgetfield(ReferenceResolutionGuard guard) {
        final ByteFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, FieldReadSnippet.ReadByte.readByte(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.CHAR)
    public static void cgetfield(ReferenceResolutionGuard guard) {
        final CharFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, FieldReadSnippet.ReadChar.readChar(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.DOUBLE)
    public static void dgetfield(ReferenceResolutionGuard guard) {
        final DoubleFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, FieldReadSnippet.ReadDouble.readDouble(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.FLOAT)
    public static void fgetfield(ReferenceResolutionGuard guard) {
        final FloatFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeFloat(0, FieldReadSnippet.ReadFloat.readFloat(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.INT)
    public static void igetfield(ReferenceResolutionGuard guard) {
        final IntFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, FieldReadSnippet.ReadInt.readInt(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.LONG)
    public static void jgetfield(ReferenceResolutionGuard guard) {
        final LongFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, FieldReadSnippet.ReadLong.readLong(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.SHORT)
    public static void sgetfield(ReferenceResolutionGuard guard) {
        final ShortFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, FieldReadSnippet.ReadShort.readShort(object, fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BOOLEAN)
    public static void zgetfield(ReferenceResolutionGuard guard) {
        final BooleanFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForReading.resolveInstanceFieldForReading(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeLoophole.booleanToByte(FieldReadSnippet.ReadBoolean.readBoolean(object, fieldActor)));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.REFERENCE)
    public static void rputfield(ReferenceResolutionGuard guard) {
        final ReferenceFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteReference.noinlineWriteReference(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.WORD)
    public static void wputfield(ReferenceResolutionGuard guard) {
        final WordFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteWord.writeWord(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BYTE)
    public static void bputfield(ReferenceResolutionGuard guard) {
        final ByteFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteByte.writeByte(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.CHAR)
    public static void cputfield(ReferenceResolutionGuard guard) {
        final CharFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteChar.writeChar(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.DOUBLE)
    public static void dputfield(ReferenceResolutionGuard guard) {
        final DoubleFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(2);
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(3);
        FieldWriteSnippet.WriteDouble.writeDouble(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.FLOAT)
    public static void fputfield(ReferenceResolutionGuard guard) {
        final FloatFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteFloat.writeFloat(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.INT)
    public static void iputfield(ReferenceResolutionGuard guard) {
        final IntFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteInt.writeInt(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.LONG)
    public static void jputfield(ReferenceResolutionGuard guard) {
        final LongFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(2);
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(3);
        FieldWriteSnippet.WriteLong.writeLong(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.SHORT)
    public static void sputfield(ReferenceResolutionGuard guard) {
        final ShortFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final short value = (short) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteShort.writeShort(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BOOLEAN)
    public static void zputfield(ReferenceResolutionGuard guard) {
        final BooleanFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveInstanceFieldForWriting.resolveInstanceFieldForWriting(guard));
        final Object object = JitStackFrameOperation.peekReference(1);
        final boolean value = UnsafeLoophole.byteToBoolean((byte) JitStackFrameOperation.peekInt(0));
        JitStackFrameOperation.removeSlots(2);
        FieldWriteSnippet.WriteBoolean.writeBoolean(object, fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BYTE)
    public static void bgetstatic(ReferenceResolutionGuard guard) {
        final ByteFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushInt(FieldReadSnippet.ReadByte.readByte(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.CHAR)
    public static void cgetstatic(ReferenceResolutionGuard guard) {
        final CharFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushInt(FieldReadSnippet.ReadChar.readChar(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.DOUBLE)
    public static void dgetstatic(ReferenceResolutionGuard guard) {
        final DoubleFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushDouble(FieldReadSnippet.ReadDouble.readDouble(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.FLOAT)
    public static void fgetstatic(ReferenceResolutionGuard guard) {
        final FloatFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushFloat(FieldReadSnippet.ReadFloat.readFloat(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.INT)
    public static void igetstatic(ReferenceResolutionGuard guard) {
        final IntFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushInt(FieldReadSnippet.ReadInt.readInt(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.LONG)
    public static void jgetstatic(ReferenceResolutionGuard guard) {
        final LongFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushLong(FieldReadSnippet.ReadLong.readLong(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.REFERENCE)
    public static void rgetstatic(ReferenceResolutionGuard guard) {
        final ReferenceFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushReference(FieldReadSnippet.ReadReference.readReference(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.SHORT)
    public static void sgetstatic(ReferenceResolutionGuard guard) {
        final ShortFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushInt(FieldReadSnippet.ReadShort.readShort(fieldActor.holder().staticTuple(), fieldActor));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BOOLEAN)
    public static void zgetstatic(ReferenceResolutionGuard guard) {
        final BooleanFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        JitStackFrameOperation.pushInt(UnsafeLoophole.booleanToByte(FieldReadSnippet.ReadBoolean.readBoolean(fieldActor.holder().staticTuple(), fieldActor)));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BYTE)
    public static void bputstatic(ReferenceResolutionGuard guard) {
        final ByteFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final byte value = (byte) JitStackFrameOperation.popInt();
        FieldWriteSnippet.WriteByte.writeByte(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.CHAR)
    public static void cputstatic(ReferenceResolutionGuard guard) {
        final CharFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final char value = (char) JitStackFrameOperation.popInt();
        FieldWriteSnippet.WriteChar.writeChar(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.DOUBLE)
    public static void dputstatic(ReferenceResolutionGuard guard) {
        final DoubleFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final double value = JitStackFrameOperation.popDouble();
        FieldWriteSnippet.WriteDouble.writeDouble(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.FLOAT)
    public static void fputstatic(ReferenceResolutionGuard guard) {
        final FloatFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final float value = JitStackFrameOperation.popFloat();
        FieldWriteSnippet.WriteFloat.writeFloat(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.INT)
    public static void iputstatic(ReferenceResolutionGuard guard) {
        final IntFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final int value = JitStackFrameOperation.popInt();
        FieldWriteSnippet.WriteInt.writeInt(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.LONG)
    public static void jputstatic(ReferenceResolutionGuard guard) {
        final LongFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final long value = JitStackFrameOperation.popLong();
        FieldWriteSnippet.WriteLong.writeLong(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.REFERENCE)
    public static void rputstatic(ReferenceResolutionGuard guard) {
        final ReferenceFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final Object value = JitStackFrameOperation.popReference();
        FieldWriteSnippet.WriteReference.noinlineWriteReference(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.SHORT)
    public static void sputstatic(ReferenceResolutionGuard guard) {
        final ShortFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final short value = (short) JitStackFrameOperation.popInt();
        FieldWriteSnippet.WriteShort.writeShort(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BOOLEAN)
    public static void zputstatic(ReferenceResolutionGuard guard) {
        final BooleanFieldActor fieldActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveStaticFieldForReading.resolveStaticFieldForReading(guard));
        final boolean value = UnsafeLoophole.byteToBoolean((byte) JitStackFrameOperation.popInt());
        FieldWriteSnippet.WriteBoolean.writeBoolean(fieldActor.holder().staticTuple(), fieldActor, value);
    }

    @INLINE
    public static void i2b() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (byte) value);
    }

    @INLINE
    public static void i2c() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (char) value);
    }

    @INLINE
    public static void i2f() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeFloat(0, value);
    }

    @INLINE
    public static void i2s() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (short) value);
    }

    @INLINE
    public static void i2l() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, value);
    }

    @INLINE
    public static void i2d() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @INLINE
    public static void iadd() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 + value2);
    }

    @INLINE
    public static void iaload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getInt(array, index));
    }

    @INLINE
    public static void iand() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 & value2);
    }

    @INLINE
    public static void iastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.noninlineCheckIndex(array, index);
        final int value = JitStackFrameOperation.peekInt(0);
        ArrayAccess.setInt(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void iconst_m1() {
        JitStackFrameOperation.pushInt(-1);
    }

    @INLINE
    public static void iconst_0() {
        JitStackFrameOperation.pushInt(0);
    }

    @INLINE
    public static void iconst_1() {
        JitStackFrameOperation.pushInt(1);
    }

    @INLINE
    public static void iconst_2() {
        JitStackFrameOperation.pushInt(2);
    }

    @INLINE
    public static void iconst_3() {
        JitStackFrameOperation.pushInt(3);
    }

    @INLINE
    public static void iconst_4() {
        JitStackFrameOperation.pushInt(4);
    }

    @INLINE
    public static void iconst_5() {
        JitStackFrameOperation.pushInt(5);
    }

    @INLINE
    public static void idiv() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 / value2);
    }

    @INLINE
    public static void iinc(int dispToLocalSlot, int increment) {
        JitStackFrameOperation.setLocalInt(dispToLocalSlot, JitStackFrameOperation.getLocalInt(dispToLocalSlot) + increment);
    }

    @INLINE
    public static void iload(int dispToLocalSlot) {
        final int value = JitStackFrameOperation.getLocalInt(dispToLocalSlot);
        JitStackFrameOperation.pushInt(value);
    }

    @INLINE
    public static void iload_0(int dispToLocalSlot) {
        iload(dispToLocalSlot);
    }

    @INLINE
    public static void iload_1(int dispToLocalSlot) {
        iload(dispToLocalSlot);
    }

    @INLINE
    public static void iload_2(int dispToLocalSlot) {
        iload(dispToLocalSlot);
    }

    @INLINE
    public static void iload_3(int dispToLocalSlot) {
        iload(dispToLocalSlot);
    }

    @INLINE
    public static void imul() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 * value2);
    }

    @INLINE
    public static void ineg() {
        JitStackFrameOperation.pokeInt(0, -JitStackFrameOperation.peekInt(0));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INSTANCEOF)
    public static void instanceof_(ReferenceResolutionGuard guard) {
        final ClassActor classActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveClass.resolveClass(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeLoophole.booleanToByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.VOID)
    public static void invokevirtualVoid(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final VirtualMethodActor dynamicMethodActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, dynamicMethodActor).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.FLOAT)
    public static void invokevirtualFloat(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final VirtualMethodActor dynamicMethodActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, dynamicMethodActor).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.LONG)
    public static void invokevirtualLong(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final VirtualMethodActor dynamicMethodActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, dynamicMethodActor).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.DOUBLE)
    public static void invokevirtualDouble(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final VirtualMethodActor dynamicMethodActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, dynamicMethodActor).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.WORD)
    public static void invokevirtualWord(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final VirtualMethodActor dynamicMethodActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveVirtualMethod.resolveVirtualMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectVirtualMethod.selectNonPrivateVirtualMethod(receiver, dynamicMethodActor).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.VOID)
    public static void invokeinterfaceVoid(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final InterfaceMethodActor declaredInterfaceMethod = UnsafeLoophole.cast(ResolveInterfaceMethod.resolveInterfaceMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.FLOAT)
    public static void invokeinterfaceFloat(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final InterfaceMethodActor declaredInterfaceMethod = UnsafeLoophole.cast(ResolveInterfaceMethod.resolveInterfaceMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.LONG)
    public static void invokeinterfaceLong(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final InterfaceMethodActor declaredInterfaceMethod = UnsafeLoophole.cast(ResolveInterfaceMethod.resolveInterfaceMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.DOUBLE)
    public static void invokeinterfaceDouble(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final InterfaceMethodActor declaredInterfaceMethod = UnsafeLoophole.cast(ResolveInterfaceMethod.resolveInterfaceMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.WORD)
    public static void invokeinterfaceWord(ReferenceResolutionGuard guard, int receiverStackIndex) {
        final InterfaceMethodActor declaredInterfaceMethod = UnsafeLoophole.cast(ResolveInterfaceMethod.resolveInterfaceMethod(guard));
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = MethodSelectionSnippet.SelectInterfaceMethod.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.VOID)
    public static void invokespecialVoid(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveSpecialMethod.resolveSpecialMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.FLOAT)
    public static void invokespecialFloat(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveSpecialMethod.resolveSpecialMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.LONG)
    public static void invokespecialLong(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveSpecialMethod.resolveSpecialMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.DOUBLE)
    public static void invokespecialDouble(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveSpecialMethod.resolveSpecialMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.WORD)
    public static void invokespecialWord(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveSpecialMethod.resolveSpecialMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.VOID)
    public static void invokestaticVoid(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveStaticMethod.resolveStaticMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.FLOAT)
    public static void invokestaticFloat(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveStaticMethod.resolveStaticMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.LONG)
    public static void invokestaticLong(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveStaticMethod.resolveStaticMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.DOUBLE)
    public static void invokestaticDouble(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveStaticMethod.resolveStaticMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.WORD)
    public static void invokestaticWord(EntrypointResolutionGuard guard) {
        final Address entryPoint = ResolveStaticMethod.resolveStaticMethod(guard).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    public static void ior() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 | value2);
    }

    @INLINE
    public static void irem() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 % value2);
    }

    @INLINE
    public static int ireturn() {
        return JitStackFrameOperation.popInt();
    }

    @INLINE
    public static void ishl() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 << value2);
    }

    @INLINE
    public static void ishr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 >> value2);
    }

    @INLINE
    public static void istore(int displacementToSlot) {
        JitStackFrameOperation.setLocalInt(displacementToSlot, JitStackFrameOperation.popInt());
    }

    @INLINE
    public static void istore_0(int displacementToSlot) {
        istore(displacementToSlot);
    }

    @INLINE
    public static void istore_1(int displacementToSlot) {
        istore(displacementToSlot);
    }

    @INLINE
    public static void istore_2(int displacementToSlot) {
        istore(displacementToSlot);
    }

    @INLINE
    public static void istore_3(int displacementToSlot) {
        istore(displacementToSlot);
    }

    @INLINE
    public static void isub() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 - value2);
    }

    @INLINE
    public static void iushr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 >>> value2);
    }

    @INLINE
    public static void ixor() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 ^ value2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.INT)
    public static void ildc(int constant) {
        JitStackFrameOperation.pushInt(constant);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.FLOAT)
    public static void fldc(float constant) {
        JitStackFrameOperation.pushFloat(constant);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.REFERENCE)
    public static void unresolved_class_ldc(ReferenceResolutionGuard guard) {
        Object mirror;
        final ClassActor classActor = UnsafeLoophole.cast(guard.value());
        if (classActor == null) {
            mirror = TemplateRuntime.resolveMirror(guard);
        } else {
            mirror = classActor.uncheckedGetMirror();
            if (mirror == null) {
                mirror = TemplateRuntime.getClassMirror(classActor);
            }
        }
        JitStackFrameOperation.pushReference(mirror);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.LONG)
    public static void jldc(long value) {
        JitStackFrameOperation.pushLong(value);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.DOUBLE)
    public static void dldc(double value) {
        JitStackFrameOperation.pushDouble(value);
    }

    @INLINE
    public static void l2d() {
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @INLINE
    public static void l2f() {
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value);
    }

    @INLINE
    public static void l2i() {
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @INLINE
    public static void ladd() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 + value2);
    }

    @INLINE
    public static void laload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.pokeLong(0, ArrayAccess.getLong(array, index));
    }

    @INLINE
    public static void land() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 & value2);
    }

    @INLINE
    public static void lastore() {
        final int index = JitStackFrameOperation.peekInt(2);
        final Object array = JitStackFrameOperation.peekReference(3);
        ArrayAccess.noninlineCheckIndex(array, index);
        final long value = JitStackFrameOperation.peekLong(0);
        ArrayAccess.setLong(array, index, value);
        JitStackFrameOperation.removeSlots(4);
    }

    @INLINE
    public static void lcmp() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        final int result = JavaBuiltin.LongCompare.longCompare(value1, value2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @INLINE
    public static void lconst_0(long zero) {
        JitStackFrameOperation.pushLong(zero);
    }

    @INLINE
    public static void lconst_1(long one) {
        JitStackFrameOperation.pushLong(one);
    }

    @INLINE
    public static void lload(int displacementToSlot) {
        JitStackFrameOperation.pushLong(JitStackFrameOperation.getLocalLong(displacementToSlot));
    }

    @INLINE
    public static void lload_0(int displacementToSlot) {
        lload(displacementToSlot);
    }

    @INLINE
    public static void lload_1(int displacementToSlot) {
        lload(displacementToSlot);
    }

    @INLINE
    public static void lload_2(int displacementToSlot) {
        lload(displacementToSlot);
    }

    @INLINE
    public static void lload_3(int displacementToSlot) {
        lload(displacementToSlot);
    }

    @INLINE
    public static void ldiv() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 / value2);
    }

    @INLINE
    public static void lmul() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 * value2);
    }

    @INLINE
    public static void lneg() {
        JitStackFrameOperation.pokeLong(0, -JitStackFrameOperation.peekLong(0));
    }

    @INLINE
    public static void lor() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 | value2);
    }

    @INLINE
    public static void lrem() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 % value2);
    }

    @INLINE
    public static long lreturn() {
        return JitStackFrameOperation.popLong();
    }

    @INLINE
    public static void lshl() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 << value2);
    }

    @INLINE
    public static void lshr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 >> value2);
    }

    @INLINE
    public static void lstore(int displacementToSlot) {
        JitStackFrameOperation.setLocalLong(displacementToSlot, JitStackFrameOperation.popLong());
    }

    @INLINE
    public static void lstore_0(int displacementToSlot) {
        lstore(displacementToSlot);
    }

    @INLINE
    public static void lstore_1(int displacementToSlot) {
        lstore(displacementToSlot);
    }

    @INLINE
    public static void lstore_2(int displacementToSlot) {
        lstore(displacementToSlot);
    }

    @INLINE
    public static void lstore_3(int displacementToSlot) {
        lstore(displacementToSlot);
    }

    @INLINE
    public static void lsub() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 - value2);
    }

    @INLINE
    public static void lushr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 >>> value2);
    }

    @INLINE
    public static void lxor() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 ^ value2);
    }

    @INLINE
    public static void monitorenter() {
        final Object object = JitStackFrameOperation.popReference();
        Monitor.noninlineEnter(object);
    }

    @INLINE
    public static void monitorexit() {
        final Object object = JitStackFrameOperation.popReference();
        Monitor.noninlineExit(object);
    }

    @INLINE
    public static void multianewarray(ReferenceResolutionGuard guard, int[] lengths) {
        final int numberOfDimensions = lengths.length;
        final ArrayClassActor arrayClassActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveClass.resolveClass(guard));

        for (int i = 1; i <= numberOfDimensions; i++) {
            final int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushReference(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.NEW)
    public static void new_(ReferenceResolutionGuard guard) {
        final ClassActor classActor = UnsafeLoophole.cast(ResolutionSnippet.ResolveClass.resolveClass(guard));
        JitStackFrameOperation.pushReference(NonFoldableSnippet.CreateTupleOrHybrid.createTupleOrHybrid(classActor));
    }

    @INLINE
    public static void newarray(Kind kind) {
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, NonFoldableSnippet.CreatePrimitiveArray.createPrimitiveArray(kind, length));
    }

    @INLINE
    public static void nop() {
        // do nothing.
    }

    @INLINE
    public static void pop() {
        JitStackFrameOperation.removeSlots(1);
    }

    @INLINE
    public static void pop2() {
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.RETURN)
    @INLINE
    public static void vreturn() {
        return;
    }

    @INLINE
    public static void saload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.noninlineCheckIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getShort(array, index));
    }

    @INLINE
    public static void sastore() {
        final short value = (short) JitStackFrameOperation.peekInt(0);
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.noninlineCheckIndex(array, index);
        ArrayAccess.setShort(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void sipush(short value) {
        JitStackFrameOperation.pushInt(value);
    }

    @INLINE
    public static void swap() {
        final Word value0 = JitStackFrameOperation.peekWord(0);
        final Word value1 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pokeWord(0, value1);
        JitStackFrameOperation.pokeWord(1, value0);
    }
}
