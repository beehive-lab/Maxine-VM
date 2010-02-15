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

import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveArrayClass.*;
import static com.sun.max.vm.template.BytecodeTemplate.*;
import static com.sun.max.vm.template.source.NoninlineTemplateRuntime.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.*;
import com.sun.max.vm.compiler.snippet.MethodSelectionSnippet.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * The Java source for the code templates used by the template-based JIT compiler.
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public final class BytecodeTemplateSource {

    @BYTECODE_TEMPLATE(NOP$instrumented$MethodEntry)
    public static void nop(MethodProfile mpo) {
        // entrypoint counters count down to zero ("overflow")
        MethodInstrumentation.recordEntrypoint(mpo);
    }

    @BYTECODE_TEMPLATE(ACONST_NULL)
    public static void aconst_null() {
        JitStackFrameOperation.pushReference(null);
    }

    @BYTECODE_TEMPLATE(AALOAD)
    public static void aaload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeReference(0, ArrayAccess.getObject(array, index));
    }

    @BYTECODE_TEMPLATE(AASTORE)
    public static void aastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        final Object value = JitStackFrameOperation.peekReference(0);
        noninlineArrayStore(index, array, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @INLINE
    private static void aload_(int dispToLocalSlot) {
        final Object value = JitStackFrameOperation.getLocalReference(dispToLocalSlot);
        JitStackFrameOperation.pushReference(value);
    }

    @BYTECODE_TEMPLATE(ALOAD)
    public static void aload(int dispToLocalSlot) {
        aload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ALOAD_0)
    public static void aload_0(int dispToLocalSlot) {
        aload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ALOAD_1)
    public static void aload_1(int dispToLocalSlot) {
        aload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ALOAD_2)
    public static void aload_2(int dispToLocalSlot) {
        aload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ALOAD_3)
    public static void aload_3(int dispToLocalSlot) {
        aload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ANEWARRAY)
    public static void anewarray(ResolutionGuard guard) {
        final ArrayClassActor arrayClassActor = UnsafeCast.asArrayClassActor(resolveArrayClass(guard));
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, CreateReferenceArray.noninlineCreateReferenceArray(arrayClassActor, length));
    }

    @BYTECODE_TEMPLATE(ARETURN)
    public static Object areturn() {
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(1);
        return value;
    }

    @BYTECODE_TEMPLATE(ARRAYLENGTH)
    public static void arraylength() {
        final int length = ArrayAccess.readArrayLength(JitStackFrameOperation.peekReference(0));
        JitStackFrameOperation.pokeInt(0, length);
    }

    @INLINE
    private static void astore_(int displacementToSlot) {
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.setLocalReference(displacementToSlot, value);
    }

    @BYTECODE_TEMPLATE(ASTORE)
    public static void astore(int displacementToSlot) {
        astore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ASTORE_0)
    public static void astore_0(int displacementToSlot) {
        astore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ASTORE_1)
    public static void astore_1(int displacementToSlot) {
        astore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ASTORE_2)
    public static void astore_2(int displacementToSlot) {
        astore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ASTORE_3)
    public static void astore_3(int displacementToSlot) {
        astore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ATHROW)
    public static void athrow() {
        Throw.raise(JitStackFrameOperation.peekReference(0));
    }

    @BYTECODE_TEMPLATE(BALOAD)
    public static void baload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getByte(array, index));
    }

    @BYTECODE_TEMPLATE(BASTORE)
    public static void bastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setByte(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(BIPUSH)
    public static void bipush(byte value) {
        JitStackFrameOperation.pushInt(value);
    }

    @BYTECODE_TEMPLATE(CALOAD)
    public static void caload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getChar(array, index));
    }

    @BYTECODE_TEMPLATE(CASTORE)
    public static void castore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setChar(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(CHECKCAST)
    public static void checkcast(ResolutionGuard guard) {
        resolveAndCheckcast(guard, JitStackFrameOperation.peekReference(0));
    }

    @BYTECODE_TEMPLATE(D2F)
    public static void d2f() {
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, (float) value);
    }

    @BYTECODE_TEMPLATE(D2I)
    public static void d2i() {
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @BYTECODE_TEMPLATE(D2L)
    public static void d2l() {
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.pokeLong(0, (long) value);
    }

    @BYTECODE_TEMPLATE(DADD)
    public static void dadd() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(DALOAD)
    public static void daload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.pokeDouble(0, ArrayAccess.getDouble(array, index));
    }

    @BYTECODE_TEMPLATE(DASTORE)
    public static void dastore() {
        final int index = JitStackFrameOperation.peekInt(2);
        final Object array = JitStackFrameOperation.peekReference(3);
        ArrayAccess.checkIndex(array, index);
        final double value = JitStackFrameOperation.peekDouble(0);
        ArrayAccess.setDouble(array, index, value);
        JitStackFrameOperation.removeSlots(4);
    }

    @BYTECODE_TEMPLATE(DCMPG)
    public static void dcmpg() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, JavaBuiltin.DoubleCompareG.doubleCompareG(value1, value2));
    }

    @BYTECODE_TEMPLATE(DCMPL)
    public static void dcmpl() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, JavaBuiltin.DoubleCompareL.doubleCompareL(value1, value2));
    }

    @BYTECODE_TEMPLATE(DCONST_0)
    public static void dconst_0(double zero) {
        JitStackFrameOperation.pushDouble(zero);
    }

    @BYTECODE_TEMPLATE(DCONST_1)
    public static void dconst_1(double one) {
        JitStackFrameOperation.pushDouble(one);
    }

    @BYTECODE_TEMPLATE(DDIV)
    public static void ddiv() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 / value2);
    }

    @INLINE
    private static void dload_(int displacementToSlot) {
        JitStackFrameOperation.pushDouble(JitStackFrameOperation.getLocalDouble(displacementToSlot));
    }

    @BYTECODE_TEMPLATE(DLOAD)
    public static void dload(int displacementToSlot) {
        dload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DLOAD_0)
    public static void dload_0(int displacementToSlot) {
        dload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DLOAD_1)
    public static void dload_1(int displacementToSlot) {
        dload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DLOAD_2)
    public static void dload_2(int displacementToSlot) {
        dload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DLOAD_3)
    public static void dload_3(int displacementToSlot) {
        dload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DMUL)
    public static void dmul() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(DNEG)
    public static void dneg(double zero) {
        JitStackFrameOperation.pokeDouble(0, zero - JitStackFrameOperation.peekDouble(0));
    }

    @BYTECODE_TEMPLATE(DREM)
    public static void drem() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(DRETURN)
    public static double dreturn() {
        return JitStackFrameOperation.popDouble();
    }

    @INLINE
    private static void dstore_(int displacementToSlot) {
        JitStackFrameOperation.setLocalDouble(displacementToSlot, JitStackFrameOperation.popDouble());
    }

    @BYTECODE_TEMPLATE(DSTORE)
    public static void dstore(int displacementToSlot) {
        dstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DSTORE_0)
    public static void dstore_0(int displacementToSlot) {
        dstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DSTORE_1)
    public static void dstore_1(int displacementToSlot) {
        dstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DSTORE_2)
    public static void dstore_2(int displacementToSlot) {
        dstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DSTORE_3)
    public static void dstore_3(int displacementToSlot) {
        dstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(DSUB)
    public static void dsub() {
        final double value2 = JitStackFrameOperation.peekDouble(0);
        final double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(DUP)
    public static void dup() {
        JitStackFrameOperation.pushWord(JitStackFrameOperation.peekWord(0));
    }

    @BYTECODE_TEMPLATE(DUP_X1)
    public static void dup_x1() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pokeWord(1, value1);
        JitStackFrameOperation.pokeWord(0, value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @BYTECODE_TEMPLATE(DUP_X2)
    public static void dup_x2() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        final Word value3 = JitStackFrameOperation.peekWord(2);
        JitStackFrameOperation.pushWord(value1);
        JitStackFrameOperation.pokeWord(1, value2);
        JitStackFrameOperation.pokeWord(2, value3);
        JitStackFrameOperation.pokeWord(3, value1);
    }

    @BYTECODE_TEMPLATE(DUP2)
    public static void dup2() {
        final Word value1 = JitStackFrameOperation.peekWord(0);
        final Word value2 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @BYTECODE_TEMPLATE(DUP2_X1)
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

    @BYTECODE_TEMPLATE(DUP2_X2)
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

    @BYTECODE_TEMPLATE(F2D)
    public static void f2d() {
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @BYTECODE_TEMPLATE(F2I)
    public static void f2i() {
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @BYTECODE_TEMPLATE(F2L)
    public static void f2l() {
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, (long) value);
    }

    @BYTECODE_TEMPLATE(FADD)
    public static void fadd() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(FALOAD)
    public static void faload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, ArrayAccess.getFloat(array, index));
    }

    @BYTECODE_TEMPLATE(FASTORE)
    public static void fastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
        final float value = JitStackFrameOperation.peekFloat(0);
        ArrayAccess.setFloat(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(FCMPG)
    public static void fcmpg() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        final int result = JavaBuiltin.FloatCompareG.floatCompareG(value1, value2);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @BYTECODE_TEMPLATE(FCMPL)
    public static void fcmpl() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        final int result = JavaBuiltin.FloatCompareL.floatCompareL(value1, value2);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @BYTECODE_TEMPLATE(FCONST_0)
    public static void fconst_0(float zero) {
        JitStackFrameOperation.pushFloat(zero);
    }

    @BYTECODE_TEMPLATE(FCONST_1)
    public static void fconst_1(float one) {
        JitStackFrameOperation.pushFloat(one);
    }

    @BYTECODE_TEMPLATE(FCONST_2)
    public static void fconst_2(float two) {
        JitStackFrameOperation.pushFloat(two);
    }

    @BYTECODE_TEMPLATE(FDIV)
    public static void fdiv() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 / value2);
    }

    @INLINE
    private static void fload_(int dispToLocalSlot) {
        final float value = JitStackFrameOperation.getLocalFloat(dispToLocalSlot);
        JitStackFrameOperation.pushFloat(value);
    }

    @BYTECODE_TEMPLATE(FLOAD)
    public static void fload(int dispToLocalSlot) {
        fload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(FLOAD_0)
    public static void fload_0(int dispToLocalSlot) {
        fload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(FLOAD_1)
    public static void fload_1(int dispToLocalSlot) {
        fload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(FLOAD_2)
    public static void fload_2(int dispToLocalSlot) {
        fload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(FLOAD_3)
    public static void fload_3(int dispToLocalSlot) {
        fload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(FMUL)
    public static void fmul() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(FNEG)
    public static void fneg(float zero) {
        JitStackFrameOperation.pokeFloat(0, zero - JitStackFrameOperation.peekFloat(0));
    }

    @BYTECODE_TEMPLATE(FREM)
    public static void frem() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(FRETURN)
    public static float freturn() {
        return JitStackFrameOperation.popFloat();
    }

    @INLINE
    private static void fstore_(int displacementToSlot) {
        JitStackFrameOperation.setLocalFloat(displacementToSlot, JitStackFrameOperation.popFloat());
    }

    @BYTECODE_TEMPLATE(FSTORE)
    public static void fstore(int displacementToSlot) {
        fstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(FSTORE_0)
    public static void fstore_0(int displacementToSlot) {
        fstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(FSTORE_1)
    public static void fstore_1(int displacementToSlot) {
        fstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(FSTORE_2)
    public static void fstore_2(int displacementToSlot) {
        fstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(FSTORE_3)
    public static void fstore_3(int displacementToSlot) {
        fstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(FSUB)
    public static void fsub() {
        final float value2 = JitStackFrameOperation.peekFloat(0);
        final float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(GETFIELD$reference)
    public static void getfieldReference(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeReference(0, resolveAndGetFieldReference(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$word)
    public static void getfieldWord(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeWord(0, resolveAndGetFieldWord(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$byte)
    public static void getfieldByte(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldByte(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$char)
    public static void getfieldChar(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldChar(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$double)
    public static void getfieldDouble(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, resolveAndGetFieldDouble(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$float)
    public static void getfieldFloat(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeFloat(0, resolveAndGetFieldFloat(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$int)
    public static void getfieldInt(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldInt(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$long)
    public static void getfieldLong(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, resolveAndGetFieldLong(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$short)
    public static void getfieldShort(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldShort(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$boolean)
    public static void getfieldBoolean(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(resolveAndGetFieldBoolean(guard, object)));
    }

    @BYTECODE_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Object value = JitStackFrameOperation.peekReference(0);
        resolveAndPutFieldReference(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Word value = JitStackFrameOperation.peekWord(0);
        resolveAndPutFieldWord(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldByte(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldChar(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final double value = JitStackFrameOperation.peekDouble(0);
        resolveAndPutFieldDouble(guard, object, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final float value = JitStackFrameOperation.peekFloat(0);
        resolveAndPutFieldFloat(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final int value = JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldInt(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final long value = JitStackFrameOperation.peekLong(0);
        resolveAndPutFieldLong(guard, object, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final short value = (short) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldShort(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard guard) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final boolean value = UnsafeCast.asBoolean((byte) JitStackFrameOperation.peekInt(0));
        resolveAndPutFieldBoolean(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(GETSTATIC$byte)
    public static void getstaticByte(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticByte(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$char)
    public static void getstaticChar(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticChar(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$double)
    public static void getstaticDouble(ResolutionGuard guard) {
        JitStackFrameOperation.pushDouble(resolveAndGetStaticDouble(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$float)
    public static void getstaticFloat(ResolutionGuard guard) {
        JitStackFrameOperation.pushFloat(resolveAndGetStaticFloat(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$int)
    public static void getstaticInt(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticInt(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$long)
    public static void getstaticLong(ResolutionGuard guard) {
        JitStackFrameOperation.pushLong(resolveAndGetStaticLong(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$reference)
    public static void getstaticReference(ResolutionGuard guard) {
        JitStackFrameOperation.pushReference(resolveAndGetStaticReference(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$short)
    public static void getstaticShort(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticShort(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$boolean)
    public static void getstaticBoolean(ResolutionGuard guard) {
        JitStackFrameOperation.pushInt(UnsafeCast.asByte(resolveAndGetStaticBoolean(guard)));
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard guard) {
        resolveAndPutStaticByte(guard, (byte) JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard guard) {
        resolveAndPutStaticChar(guard, (char) JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard guard) {
        resolveAndPutStaticDouble(guard, JitStackFrameOperation.popDouble());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard guard) {
        resolveAndPutStaticFloat(guard, JitStackFrameOperation.popFloat());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard guard) {
        resolveAndPutStaticInt(guard, JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard guard) {
        resolveAndPutStaticLong(guard, JitStackFrameOperation.popLong());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard guard) {
        resolveAndPutStaticReference(guard, JitStackFrameOperation.peekReference(0));
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard guard) {
        resolveAndPutStaticShort(guard, (short) JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard guard) {
        resolveAndPutStaticBoolean(guard, UnsafeCast.asBoolean((byte) JitStackFrameOperation.popInt()));
    }

    @BYTECODE_TEMPLATE(I2B)
    public static void i2b() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (byte) value);
    }

    @BYTECODE_TEMPLATE(I2C)
    public static void i2c() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (char) value);
    }

    @BYTECODE_TEMPLATE(I2F)
    public static void i2f() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeFloat(0, value);
    }

    @BYTECODE_TEMPLATE(I2S)
    public static void i2s() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (short) value);
    }

    @BYTECODE_TEMPLATE(I2L)
    public static void i2l() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, value);
    }

    @BYTECODE_TEMPLATE(I2D)
    public static void i2d() {
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @BYTECODE_TEMPLATE(IADD)
    public static void iadd() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(IALOAD)
    public static void iaload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getInt(array, index));
    }

    @BYTECODE_TEMPLATE(IAND)
    public static void iand() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 & value2);
    }

    @BYTECODE_TEMPLATE(IASTORE)
    public static void iastore() {
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
        final int value = JitStackFrameOperation.peekInt(0);
        ArrayAccess.setInt(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(ICONST_M1)
    public static void iconst_m1() {
        JitStackFrameOperation.pushInt(-1);
    }

    @BYTECODE_TEMPLATE(ICONST_0)
    public static void iconst_0() {
        JitStackFrameOperation.pushInt(0);
    }

    @BYTECODE_TEMPLATE(ICONST_1)
    public static void iconst_1() {
        JitStackFrameOperation.pushInt(1);
    }

    @BYTECODE_TEMPLATE(ICONST_2)
    public static void iconst_2() {
        JitStackFrameOperation.pushInt(2);
    }

    @BYTECODE_TEMPLATE(ICONST_3)
    public static void iconst_3() {
        JitStackFrameOperation.pushInt(3);
    }

    @BYTECODE_TEMPLATE(ICONST_4)
    public static void iconst_4() {
        JitStackFrameOperation.pushInt(4);
    }

    @BYTECODE_TEMPLATE(ICONST_5)
    public static void iconst_5() {
        JitStackFrameOperation.pushInt(5);
    }

    @BYTECODE_TEMPLATE(IDIV)
    public static void idiv() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 / value2);
    }

    @BYTECODE_TEMPLATE(IINC)
    public static void iinc(int dispToLocalSlot, int increment) {
        JitStackFrameOperation.setLocalInt(dispToLocalSlot, JitStackFrameOperation.getLocalInt(dispToLocalSlot) + increment);
    }

    @INLINE
    public static void iload_(int dispToLocalSlot) {
        final int value = JitStackFrameOperation.getLocalInt(dispToLocalSlot);
        JitStackFrameOperation.pushInt(value);
    }

    @BYTECODE_TEMPLATE(ILOAD)
    public static void iload(int dispToLocalSlot) {
        iload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ILOAD_0)
    public static void iload_0(int dispToLocalSlot) {
        iload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ILOAD_1)
    public static void iload_1(int dispToLocalSlot) {
        iload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ILOAD_2)
    public static void iload_2(int dispToLocalSlot) {
        iload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(ILOAD_3)
    public static void iload_3(int dispToLocalSlot) {
        iload_(dispToLocalSlot);
    }

    @BYTECODE_TEMPLATE(IMUL)
    public static void imul() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(INEG)
    public static void ineg() {
        JitStackFrameOperation.pokeInt(0, -JitStackFrameOperation.peekInt(0));
    }

    @BYTECODE_TEMPLATE(INSTANCEOF)
    public static void instanceof_(ResolutionGuard guard) {
        final ClassActor classActor = UnsafeCast.asClassActor(NoninlineTemplateRuntime.resolveClass(guard));
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$void)
    public static void invokevirtualVoid(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$float)
    public static void invokevirtualFloat(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$long)
    public static void invokevirtualLong(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$double)
    public static void invokevirtualDouble(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$word)
    public static void invokevirtualWord(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$void)
    public static void invokeinterfaceVoid(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$float)
    public static void invokeinterfaceFloat(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$long)
    public static void invokeinterfaceLong(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$double)
    public static void invokeinterfaceDouble(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$word)
    public static void invokeinterfaceWord(ResolutionGuard guard, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$void)
    public static void invokespecialVoid(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallVoid(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$float)
    public static void invokespecialFloat(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallFloat(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$long)
    public static void invokespecialLong(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallLong(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$double)
    public static void invokespecialDouble(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallDouble(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$word)
    public static void invokespecialWord(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallWord(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$void)
    public static void invokestaticVoid(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallVoid(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$float)
    public static void invokestaticFloat(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallFloat(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$long)
    public static void invokestaticLong(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallLong(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$double)
    public static void invokestaticDouble(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallDouble(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$word)
    public static void invokestaticWord(ResolutionGuard guard) {
        JitStackFrameOperation.indirectCallWord(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(IOR)
    public static void ior() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 | value2);
    }

    @BYTECODE_TEMPLATE(IREM)
    public static void irem() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(IRETURN)
    public static int ireturn() {
        return JitStackFrameOperation.popInt();
    }

    @BYTECODE_TEMPLATE(ISHL)
    public static void ishl() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 << value2);
    }

    @BYTECODE_TEMPLATE(ISHR)
    public static void ishr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 >> value2);
    }

    @INLINE
    private static void istore_(int displacementToSlot) {
        JitStackFrameOperation.setLocalInt(displacementToSlot, JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(ISTORE)
    public static void istore(int displacementToSlot) {
        istore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ISTORE_0)
    public static void istore_0(int displacementToSlot) {
        istore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ISTORE_1)
    public static void istore_1(int displacementToSlot) {
        istore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ISTORE_2)
    public static void istore_2(int displacementToSlot) {
        istore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ISTORE_3)
    public static void istore_3(int displacementToSlot) {
        istore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(ISUB)
    public static void isub() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(IUSHR)
    public static void iushr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 >>> value2);
    }

    @BYTECODE_TEMPLATE(IXOR)
    public static void ixor() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 ^ value2);
    }

    @BYTECODE_TEMPLATE(LDC$int)
    public static void ildc(int constant) {
        JitStackFrameOperation.pushInt(constant);
    }

    @BYTECODE_TEMPLATE(LDC$float)
    public static void fldc(float constant) {
        JitStackFrameOperation.pushFloat(constant);
    }

    @BYTECODE_TEMPLATE(LDC$reference)
    public static void unresolved_class_ldc(ResolutionGuard guard) {
        final ClassActor classActor = NoninlineTemplateRuntime.resolveClass(guard);
        final Object mirror = NoninlineTemplateRuntime.getClassMirror(classActor);
        JitStackFrameOperation.pushReference(mirror);
    }

    @BYTECODE_TEMPLATE(LDC$long)
    public static void jldc(long value) {
        JitStackFrameOperation.pushLong(value);
    }

    @BYTECODE_TEMPLATE(LDC$double)
    public static void dldc(double value) {
        JitStackFrameOperation.pushDouble(value);
    }

    @BYTECODE_TEMPLATE(L2D)
    public static void l2d() {
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @BYTECODE_TEMPLATE(L2F)
    public static void l2f() {
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value);
    }

    @BYTECODE_TEMPLATE(L2I)
    public static void l2i() {
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @BYTECODE_TEMPLATE(LADD)
    public static void ladd() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(LALOAD)
    public static void laload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.pokeLong(0, ArrayAccess.getLong(array, index));
    }

    @BYTECODE_TEMPLATE(LAND)
    public static void land() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 & value2);
    }

    @BYTECODE_TEMPLATE(LASTORE)
    public static void lastore() {
        final int index = JitStackFrameOperation.peekInt(2);
        final Object array = JitStackFrameOperation.peekReference(3);
        ArrayAccess.checkIndex(array, index);
        final long value = JitStackFrameOperation.peekLong(0);
        ArrayAccess.setLong(array, index, value);
        JitStackFrameOperation.removeSlots(4);
    }

    @BYTECODE_TEMPLATE(LCMP)
    public static void lcmp() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        final int result = JavaBuiltin.LongCompare.longCompare(value1, value2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @BYTECODE_TEMPLATE(LCONST_0)
    public static void lconst_0(long zero) {
        JitStackFrameOperation.pushLong(zero);
    }

    @BYTECODE_TEMPLATE(LCONST_1)
    public static void lconst_1(long one) {
        JitStackFrameOperation.pushLong(one);
    }

    @INLINE
    private static void lload_(int displacementToSlot) {
        JitStackFrameOperation.pushLong(JitStackFrameOperation.getLocalLong(displacementToSlot));
    }

    @BYTECODE_TEMPLATE(LLOAD)
    public static void lload(int displacementToSlot) {
        lload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LLOAD_0)
    public static void lload_0(int displacementToSlot) {
        lload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LLOAD_1)
    public static void lload_1(int displacementToSlot) {
        lload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LLOAD_2)
    public static void lload_2(int displacementToSlot) {
        lload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LLOAD_3)
    public static void lload_3(int displacementToSlot) {
        lload_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LDIV)
    public static void ldiv() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 / value2);
    }

    @BYTECODE_TEMPLATE(LMUL)
    public static void lmul() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(LNEG)
    public static void lneg() {
        JitStackFrameOperation.pokeLong(0, -JitStackFrameOperation.peekLong(0));
    }

    @BYTECODE_TEMPLATE(LOR)
    public static void lor() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 | value2);
    }

    @BYTECODE_TEMPLATE(LREM)
    public static void lrem() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(LRETURN)
    public static long lreturn() {
        return JitStackFrameOperation.popLong();
    }

    @BYTECODE_TEMPLATE(LSHL)
    public static void lshl() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 << value2);
    }

    @BYTECODE_TEMPLATE(LSHR)
    public static void lshr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 >> value2);
    }

    @INLINE
    private static void lstore_(int displacementToSlot) {
        JitStackFrameOperation.setLocalLong(displacementToSlot, JitStackFrameOperation.popLong());
    }

    @BYTECODE_TEMPLATE(LSTORE)
    public static void lstor(int displacementToSlot) {
        lstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LSTORE_0)
    public static void lstore_0(int displacementToSlot) {
        lstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LSTORE_1)
    public static void lstore_1(int displacementToSlot) {
        lstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LSTORE_2)
    public static void lstore_2(int displacementToSlot) {
        lstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LSTORE_3)
    public static void lstore_3(int displacementToSlot) {
        lstore_(displacementToSlot);
    }

    @BYTECODE_TEMPLATE(LSUB)
    public static void lsub() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(LUSHR)
    public static void lushr() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 >>> value2);
    }

    @BYTECODE_TEMPLATE(LXOR)
    public static void lxor() {
        final long value2 = JitStackFrameOperation.peekLong(0);
        final long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 ^ value2);
    }

    @BYTECODE_TEMPLATE(MONITORENTER)
    public static void monitorenter() {
        final Object object = JitStackFrameOperation.peekReference(0);
        Monitor.noninlineEnter(object);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(MONITOREXIT)
    public static void monitorexit() {
        final Object object = JitStackFrameOperation.peekReference(0);
        Monitor.noninlineExit(object);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(MULTIANEWARRAY)
    public static void multianewarray(ResolutionGuard guard, int[] lengthsShared) {
        final ClassActor arrayClassActor = NoninlineTemplateRuntime.resolveClass(guard);

        // Need to use an unsafe cast to remove the checkcast inserted by javac as that causes this
        // template to have a reference literal in its compiled form.
        final int[] lengths = UnsafeCast.asIntArray(lengthsShared.clone());
        final int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            final int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushReference(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }

    @BYTECODE_TEMPLATE(NEW)
    public static void new_(ResolutionGuard guard) {
        JitStackFrameOperation.pushReference(resolveClassForNewAndCreate(guard));
    }

    @BYTECODE_TEMPLATE(NEWARRAY)
    public static void newarray(Kind kind) {
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, CreatePrimitiveArray.noninlineCreatePrimitiveArray(kind, length));
    }

    @BYTECODE_TEMPLATE(NOP)
    public static void nop() {
        // do nothing.
    }

    @BYTECODE_TEMPLATE(POP)
    public static void pop() {
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(POP2)
    public static void pop2() {
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(RETURN)
    public static void vreturn() {
        return;
    }

    @BYTECODE_TEMPLATE(SALOAD)
    public static void saload() {
        final int index = JitStackFrameOperation.peekInt(0);
        final Object array = JitStackFrameOperation.peekReference(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getShort(array, index));
    }

    @BYTECODE_TEMPLATE(SASTORE)
    public static void sastore() {
        final short value = (short) JitStackFrameOperation.peekInt(0);
        final int index = JitStackFrameOperation.peekInt(1);
        final Object array = JitStackFrameOperation.peekReference(2);
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.setShort(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(SIPUSH)
    public static void sipush(short value) {
        JitStackFrameOperation.pushInt(value);
    }

    @BYTECODE_TEMPLATE(SWAP)
    public static void swap() {
        final Word value0 = JitStackFrameOperation.peekWord(0);
        final Word value1 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pokeWord(0, value1);
        JitStackFrameOperation.pokeWord(1, value0);
    }


    /*
     * Templates for conditional branch bytecode instructions.
     *
     * These templates only comprise the prefix of a conditional branch: popping operands of the comparison and the comparison itself.
     * They have no dependencies, i.e., they can be copied as is by the bytecode-to-target translator of the JIT. The actual branching
     * is emitted by the JIT. Templates for the same family of bytecodes are identical (what typically makes them different is the condition being tested).
     * The templates relies on two special builtins for comparing issuing an object comparison and a integer comparison. These are specific to template generation.
     */

    @INLINE
    private static void icmp0_prefix() {
        SpecialBuiltin.compareInts(JitStackFrameOperation.popInt(), 0);
    }

    @INLINE
    private static void acmp0_prefix() {
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(1);
        SpecialBuiltin.compareReferences(value, null);
    }

    @INLINE
    private static void icmp_prefix() {
        final int value2 = JitStackFrameOperation.peekInt(0);
        final int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(2);
        SpecialBuiltin.compareInts(value1, value2);
    }

    @INLINE
    private static void acmp_prefix() {
        final Object value2 = JitStackFrameOperation.peekReference(0);
        final Object value1 = JitStackFrameOperation.peekReference(1);
        JitStackFrameOperation.removeSlots(2);
        SpecialBuiltin.compareReferences(value1, value2);
    }

    @BYTECODE_TEMPLATE(IF_ACMPEQ)
    public static void if_acmpeq() {
        acmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ACMPNE)
    public static void if_acmpne() {
        acmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ICMPEQ)
    public static void if_icmpeq() {
        icmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ICMPNE)
    public static void if_icmpne() {
        icmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ICMPLT)
    public static void if_icmplt() {
        icmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ICMPGE)
    public static void if_icmpge() {
        icmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ICMPGT)
    public static void if_icmpgt() {
        icmp_prefix();
    }

    @BYTECODE_TEMPLATE(IF_ICMPLE)
    public static void if_icmple() {
        icmp_prefix();
    }

    @BYTECODE_TEMPLATE(IFEQ)
    public static void ifeq() {
        icmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFNE)
    public static void ifne() {
        icmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFLT)
    public static void iflt() {
        icmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFGE)
    public static void ifge() {
        icmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFGT)
    public static void ifgt() {
        icmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFLE)
    public static void ifle() {
        icmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFNONNULL)
    public static void ifnonnull() {
        acmp0_prefix();
    }

    @BYTECODE_TEMPLATE(IFNULL)
    public static void ifnull() {
        acmp0_prefix();
    }

    // Instructions with an initialized class operand

    @BYTECODE_TEMPLATE(NEW$init)
    public static void new_(ClassActor classActor) {
        JitStackFrameOperation.pushReference(NoninlineTemplateRuntime.noninlineNew(classActor));
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$void$init)
    public static void invokestatic() {
        JitStackFrameOperation.directCallVoid();
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$float$init)
    public static void invokestaticFloat() {
        JitStackFrameOperation.directCallFloat();
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$long$init)
    public static void invokestaticLong() {
        JitStackFrameOperation.directCallLong();
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$double$init)
    public static void invokestaticDouble() {
        JitStackFrameOperation.directCallDouble();
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$word$init)
    public static void invokestaticWord() {
        JitStackFrameOperation.directCallWord();
    }

    @BYTECODE_TEMPLATE(GETSTATIC$byte$init)
    public static void getstaticByte(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readByte(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$char$init)
    public static void getstaticChar(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readChar(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$double$init)
    public static void getstaticDouble(Object staticTuple, int offset) {
        JitStackFrameOperation.pushDouble(TupleAccess.readDouble(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$float$init)
    public static void getstaticFloat(Object staticTuple, int offset) {
        JitStackFrameOperation.pushFloat(TupleAccess.readFloat(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$int$init)
    public static void getstaticInt(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readInt(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$long$init)
    public static void getstaticLong(Object staticTuple, int offset) {
        JitStackFrameOperation.pushLong(TupleAccess.readLong(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$reference$init)
    public static void getstaticReference(Object staticTuple, int offset) {
        JitStackFrameOperation.pushReference(TupleAccess.readObject(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$short$init)
    public static void getstaticShort(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(TupleAccess.readShort(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$boolean$init)
    public static void getstaticBoolean(Object staticTuple, int offset) {
        JitStackFrameOperation.pushInt(UnsafeCast.asByte(TupleAccess.readBoolean(staticTuple, offset)));
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$byte$init)
    public static void putstaticByte(Object staticTuple, int offset) {
        final byte value = (byte) JitStackFrameOperation.popInt();
        TupleAccess.writeByte(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$char$init)
    public static void putstaticChar(Object staticTuple, int offset) {
        final char value = (char) JitStackFrameOperation.popInt();
        TupleAccess.writeChar(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$double$init)
    public static void putstaticDouble(Object staticTuple, int offset) {
        final double value = JitStackFrameOperation.popDouble();
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$float$init)
    public static void putstaticFloat(Object staticTuple, int offset) {
        final float value = JitStackFrameOperation.popFloat();
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$int$init)
    public static void putstaticInt(Object staticTuple, int offset) {
        final int value = JitStackFrameOperation.popInt();
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$long$init)
    public static void putstaticLong(Object staticTuple, int offset) {
        final long value = JitStackFrameOperation.popLong();
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$reference$init)
    public static void putstaticReference(Object staticTuple, int offset) {
        final Object value = JitStackFrameOperation.peekReference(0);
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$short$init)
    public static void putstaticShort(Object staticTuple, int offset) {
        final short value = (short) JitStackFrameOperation.popInt();
        TupleAccess.writeShort(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$boolean$init)
    public static void putstaticBoolean(Object staticTuple, int offset) {
        final boolean value = UnsafeCast.asBoolean((byte) JitStackFrameOperation.popInt());
        TupleAccess.writeBoolean(staticTuple, offset, value);
    }

    // Instructions with a resolved class operand

    @BYTECODE_TEMPLATE(LDC$reference$resolved)
    public static void rldc(Object value) {
        JitStackFrameOperation.pushReference(value);
    }

    @BYTECODE_TEMPLATE(CHECKCAST$resolved)
    public static void checkcast(ClassActor classActor) {
        Snippet.CheckCast.checkCast(classActor, JitStackFrameOperation.peekReference(0));
    }

    @BYTECODE_TEMPLATE(INSTANCEOF$resolved)
    public static void instanceof_(ClassActor classActor) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @BYTECODE_TEMPLATE(ANEWARRAY$resolved)
    public static void anewarray(ArrayClassActor arrayClassActor) {
        final int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, CreateReferenceArray.noninlineCreateReferenceArray(arrayClassActor, length));
    }

    @BYTECODE_TEMPLATE(MULTIANEWARRAY$resolved)
    public static void multianewarray(ArrayClassActor arrayClassActor, int[] lengthsShared) {
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that causes this
        // template to have a reference literal in its compiled form.
        final int[] lengths = UnsafeCast.asIntArray(lengthsShared.clone());
        final int numberOfDimensions = lengths.length;
        for (int i = 1; i <= numberOfDimensions; i++) {
            final int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushReference(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$reference$resolved)
    public static void getfieldReference(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeReference(0, TupleAccess.readObject(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$word$resolved)
    public static void getfieldWord(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeWord(0, TupleAccess.readWord(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$byte$resolved)
    public static void getfieldByte(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readByte(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$char$resolved)
    public static void getfieldChar(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readChar(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$double$resolved)
    public static void getfieldDouble(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, TupleAccess.readDouble(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$float$resolved)
    public static void getfieldFloat(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeFloat(0, TupleAccess.readFloat(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$int$resolved)
    public static void getfieldInt(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readInt(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$long$resolved)
    public static void getfieldLong(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, TupleAccess.readLong(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$short$resolved)
    public static void getfieldShort(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readShort(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$boolean$resolved)
    public static void getfieldBoolean(int offset) {
        final Object object = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(TupleAccess.readBoolean(object, offset)));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$reference$resolved)
    public static void putfieldReference(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Object value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$word$resolved)
    public static void putfieldWord(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeWord(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$byte$resolved)
    public static void putfieldByte(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final byte value = (byte) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeByte(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$char$resolved)
    public static void putfieldChar(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final char value = (char) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeChar(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$double$resolved)
    public static void putfieldDouble(int offset) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(3);
        TupleAccess.writeDouble(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$float$resolved)
    public static void putfieldFloat(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeFloat(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$int$resolved)
    public static void putfieldInt(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeInt(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$long$resolved)
    public static void putfieldLong(int offset) {
        final Object object = JitStackFrameOperation.peekReference(2);
        final long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(3);
        TupleAccess.writeLong(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$short$resolved)
    public static void putfieldShort(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final short value = (short) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeShort(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(int offset) {
        final Object object = JitStackFrameOperation.peekReference(1);
        final boolean value = UnsafeCast.asBoolean((byte) JitStackFrameOperation.peekInt(0));
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeBoolean(object, offset, value);
    }

    /**
     * Template sources for the invocation of resolved methods.
     * <p>
     * The calling convention is such that the part of the callee's frame that contains the incoming arguments of the method
     * is the top of the stack of the caller. The rest of the frame is built on method entry. Thus, the template for a
     * method invocation doesn't need to marshal the arguments. We can't just have templates compiled from standard method
     * invocations as the compiler mixes instructions for arguments passing and method invocation. So we have to write
     * template such that the call is explicitly made (using the Call SpecialBuiltin). Further, we need a template for each
     * of the four kinds of returned result (void, one word, two words, a reference).
     * <p>
     * For methods with a static binding (e.g., methods invoked via invokestatic or invokespecial), we just need to issue a
     * call. Thus a template for these bytecode is a single call instruction. A template resulting in this can be achieved
     * by invoke a parameterless static method of an initialized class. We generate these templates for completion, although
     * in practice a JIT might be better off generating the call instruction directly.
     * <p>
     * For dynamic method, the receiver is needed and method dispatch need to be generated. For the template, we pick an
     * object at an arbitrary position on the expression stack to be the receiver, and rely on the optimizing compiler to
     * generate dependency information about the constant value used as offset to read off the expression stack. JIT
     * compilers just have to modify this offset using the appropriate instruction modifier from the generated template.
     * Similarly, JIT compilers have to customized the virtual table index / itable serial identifier. Note that we use a
     * parameter-less void method in the expression performing dynamic method selection, regardless of the number of
     * parameters or of the kind of return value the template is to be used for.
     *
     * @author Laurent Daynes
     */
    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$void$resolved)
    public static void invokevirtual(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$float$resolved)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$long$resolved)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$double$resolved)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$word$resolved)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$void$resolved)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$float$resolved)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$long$resolved)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$double$resolved)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$word$resolved)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKESPECIAL$void$resolved)
    public static void invokespecial() {
        JitStackFrameOperation.directCallVoid();
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKESPECIAL$float$resolved)
    public static void invokespecialReturnSingleSlot() {
        JitStackFrameOperation.directCallFloat();
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKESPECIAL$long$resolved)
    public static void invokespecialLong() {
        JitStackFrameOperation.directCallLong();
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKESPECIAL$double$resolved)
    public static void invokespecialDouble() {
        JitStackFrameOperation.directCallDouble();
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKESPECIAL$word$resolved)
    public static void invokespecialWord() {
        JitStackFrameOperation.directCallWord();
    }

    // Templates for instrumented invokevirtual and invokeinterface instructions.

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$void$instrumented)
    public static void invokevirtual(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$float$instrumented)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$long$instrumented)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$double$instrumented)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$word$instrumented)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$void$instrumented)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$float$instrumented)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$long$instrumented)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$double$instrumented)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$word$instrumented)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        final Object receiver = JitStackFrameOperation.peekReference(receiverStackIndex);
        final Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    private static Address selectVirtualMethod(final Object receiver, int vTableIndex, MethodProfile mpo, int mpoIndex) {
        final Hub hub = ObjectAccess.readHub(receiver);
        final Address entryPoint = hub.getWord(vTableIndex).asAddress();
        MethodInstrumentation.recordType(mpo, hub, mpoIndex, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES);
        return entryPoint;
    }

    @INLINE
    private static Address selectInterfaceMethod(final Object receiver, InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex) {
        final Hub hub = ObjectAccess.readHub(receiver);
        final Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        MethodInstrumentation.recordType(mpo, hub, mpoIndex, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES);
        return entryPoint;
    }
}
