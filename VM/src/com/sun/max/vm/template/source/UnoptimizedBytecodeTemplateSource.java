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

import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveArrayClass.*;
import static com.sun.max.vm.template.source.NoninlineTemplateRuntime.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.NonFoldableSnippet.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
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
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeReference(0, ArrayAccess.getObject(array, index));
    }

    @INLINE
    public static void aastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        final Object value = JitStackFrameOperation.peekReference(0);
        noninlineArrayStore(index, array, value);
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
    public static void anewarray(ResolutionGuard guard) {
        final ArrayClassActor arrayClassActor = UnsafeLoophole.asArrayClassActor(resolveArrayClass(guard));
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, NonFoldableSnippet.CreateReferenceArray.noninlineCreateReferenceArray(arrayClassActor, length));
    }

    @INLINE
    public static Object areturn() {
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(1);
        return value;
    }

    @INLINE
    public static void arraylength() {
        final int length = ArrayAccess.readArrayLength(JitStackFrameOperation.peekReference(0));
        JitStackFrameOperation.pokeInt(0, length);
    }

    @INLINE
    public static void astore(int displacementToSlot) {
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.setLocalReference(displacementToSlot, value);
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
        Throw.raise(JitStackFrameOperation.peekReference(0));
    }

    @INLINE
    public static void baload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getByte(array, index));
    }

    @INLINE
    public static void bastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
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
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getChar(array, index));
    }

    @INLINE
    public static void castore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setChar(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    public static void checkcast(ResolutionGuard guard) {
        resolveAndCheckcast(guard, JitStackFrameOperation.peekReference(0));
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
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.pokeDouble(0, ArrayAccess.getDouble(array, index));
    }

    @INLINE
    public static void dastore() {
        final int index = JitStackFrameOperation.peekInt(2);
        final Object array = JitStackFrameOperation.peekReference(3);
        ArrayAccess.checkIndex(array, index);
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
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, ArrayAccess.getFloat(array, index));
    }

    @INLINE
    public static void fastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
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
    public static void rgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeReference(0, resolveAndGetFieldReference(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.WORD)
    public static void wgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeWord(0, resolveAndGetFieldWord(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BYTE)
    public static void bgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldByte(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.CHAR)
    public static void cgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldChar(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.DOUBLE)
    public static void dgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, resolveAndGetFieldDouble(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.FLOAT)
    public static void fgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeFloat(0, resolveAndGetFieldFloat(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.INT)
    public static void igetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldInt(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.LONG)
    public static void jgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, resolveAndGetFieldLong(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.SHORT)
    public static void sgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldShort(guard, object));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BOOLEAN)
    public static void zgetfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeLoophole.asByte(resolveAndGetFieldBoolean(guard, object)));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.REFERENCE)
    public static void rputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Object value = JitStackFrameOperation.peekReference(0);
        resolveAndPutFieldReference(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.WORD)
    public static void wputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Word value = JitStackFrameOperation.peekWord(0);
        resolveAndPutFieldWord(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BYTE)
    public static void bputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldByte(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.CHAR)
    public static void cputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldChar(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.DOUBLE)
    public static void dputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final double value = JitStackFrameOperation.peekDouble(0);
        resolveAndPutFieldDouble(guard, object, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.FLOAT)
    public static void fputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final float value = JitStackFrameOperation.peekFloat(0);
        resolveAndPutFieldFloat(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.INT)
    public static void iputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final int value = JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldInt(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.LONG)
    public static void jputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final long value = JitStackFrameOperation.peekLong(0);
        resolveAndPutFieldLong(guard, object, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.SHORT)
    public static void sputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final short value = (short) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldShort(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BOOLEAN)
    public static void zputfield(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final boolean value = UnsafeLoophole.asBoolean((byte) JitStackFrameOperation.peekInt(0));
        resolveAndPutFieldBoolean(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BYTE)
    public static void bgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticByte(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.CHAR)
    public static void cgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticChar(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.DOUBLE)
    public static void dgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushDouble(resolveAndGetStaticDouble(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.FLOAT)
    public static void fgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushFloat(resolveAndGetStaticFloat(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.INT)
    public static void igetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticInt(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.LONG)
    public static void jgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushLong(resolveAndGetStaticLong(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.REFERENCE)
    public static void rgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushReference(resolveAndGetStaticReference(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.SHORT)
    public static void sgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticShort(guard));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BOOLEAN)
    public static void zgetstatic(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(UnsafeLoophole.asByte(resolveAndGetStaticBoolean(guard)));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BYTE)
    public static void bputstatic(ResolutionGuard guard) {
        resolveAndPutStaticByte(guard, (byte) JitStackFrameOperation.popInt());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.CHAR)
    public static void cputstatic(ResolutionGuard guard) {
        resolveAndPutStaticChar(guard, (char) JitStackFrameOperation.popInt());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.DOUBLE)
    public static void dputstatic(ResolutionGuard guard) {
        resolveAndPutStaticDouble(guard, JitStackFrameOperation.popDouble());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.FLOAT)
    public static void fputstatic(ResolutionGuard guard) {
        resolveAndPutStaticFloat(guard, JitStackFrameOperation.popFloat());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.INT)
    public static void iputstatic(ResolutionGuard guard) {
        resolveAndPutStaticInt(guard, JitStackFrameOperation.popInt());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.LONG)
    public static void jputstatic(ResolutionGuard guard) {
        resolveAndPutStaticLong(guard, JitStackFrameOperation.popLong());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.REFERENCE)
    public static void rputstatic(ResolutionGuard guard) {
        resolveAndPutStaticReference(guard, JitStackFrameOperation.peekReference(0));
        JitStackFrameOperation.removeSlots(1);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.SHORT)
    public static void sputstatic(ResolutionGuard guard) {
        resolveAndPutStaticShort(guard, (short) JitStackFrameOperation.popInt());
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BOOLEAN)
    public static void zputstatic(ResolutionGuard guard) {
        resolveAndPutStaticBoolean(guard, UnsafeLoophole.asBoolean((byte) JitStackFrameOperation.popInt()));
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
        ArrayAccess.checkIndex(array, index);
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
        ArrayAccess.checkIndex(array, index);
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
    public static void instanceof_(ResolutionGuard guard) {
        final ClassActor classActor = UnsafeLoophole.asClassActor(NoninlineTemplateRuntime.resolveClass(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeLoophole.asByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.VOID)
    public static void invokevirtualVoid(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.FLOAT)
    public static void invokevirtualFloat(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.LONG)
    public static void invokevirtualLong(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.DOUBLE)
    public static void invokevirtualDouble(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.WORD)
    public static void invokevirtualWord(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.VOID)
    public static void invokeinterfaceVoid(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.FLOAT)
    public static void invokeinterfaceFloat(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.LONG)
    public static void invokeinterfaceLong(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.DOUBLE)
    public static void invokeinterfaceDouble(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.WORD)
    public static void invokeinterfaceWord(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.VOID)
    public static void invokespecialVoid(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallVoid(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.FLOAT)
    public static void invokespecialFloat(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallFloat(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.LONG)
    public static void invokespecialLong(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallLong(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.DOUBLE)
    public static void invokespecialDouble(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallDouble(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.WORD)
    public static void invokespecialWord(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallWord(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.VOID)
    public static void invokestaticVoid(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallVoid(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.FLOAT)
    public static void invokestaticFloat(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallFloat(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.LONG)
    public static void invokestaticLong(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallLong(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.DOUBLE)
    public static void invokestaticDouble(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallDouble(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.WORD)
    public static void invokestaticWord(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallWord(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
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
    public static void unresolved_class_ldc(ResolutionGuard guard) {
        final ClassActor classActor = NoninlineTemplateRuntime.resolveClass(guard);
        final Object mirror = NoninlineTemplateRuntime.getClassMirror(classActor);
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
        ArrayAccess.checkIndex(array, index);
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
        ArrayAccess.checkIndex(array, index);
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
        final Object object = JitStackFrameOperation.peekReference(0);
        Monitor.noninlineEnter(object);
        JitStackFrameOperation.removeSlots(1);
    }

    @INLINE
    public static void monitorexit() {
        final Object object = JitStackFrameOperation.peekReference(0);
        Monitor.noninlineExit(object);
        JitStackFrameOperation.removeSlots(1);
    }

    @INLINE
    public static void multianewarray(ResolutionGuard guard, int[] lengthsShared) {
        final ClassActor arrayClassActor = NoninlineTemplateRuntime.resolveClass(guard);

        // Need to use an unsafe cast to remove the checkcast inserted by javac as that causes this
        // template to have a reference literal in its compiled form.
        final int[] lengths = UnsafeLoophole.asIntArray(lengthsShared.clone());
        final int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            final int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushReference(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }

    @INLINE
    @BYTECODE_TEMPLATE(bytecode = Bytecode.NEW)
    public static void new_(ResolutionGuard guard) {
        JitStackFrameOperation.pushReference(resolveClassForNewAndCreate(guard));
    }

    @INLINE
    public static void newarray(Kind kind) {
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, NonFoldableSnippet.CreatePrimitiveArray.noninlineCreatePrimitiveArray(kind, length));
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
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getShort(array, index));
    }

    @INLINE
    public static void sastore() {
        final short value = (short) JitStackFrameOperation.peekInt(0);
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
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
