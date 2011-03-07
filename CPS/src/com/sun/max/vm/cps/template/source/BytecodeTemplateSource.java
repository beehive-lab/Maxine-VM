/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.template.source;

import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveArrayClass.*;
import static com.sun.max.vm.cps.template.BytecodeTemplate.*;
import static com.sun.max.vm.cps.template.source.NoninlineTemplateRuntime.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreateMultiReferenceArray;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreatePrimitiveArray;
import com.sun.max.vm.compiler.snippet.CreateArraySnippet.CreateReferenceArray;
import com.sun.max.vm.compiler.snippet.MethodSelectionSnippet.SelectInterfaceMethod;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveClass;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The Java source for the code templates used by the template-based JIT compiler.
 *
 * @author Laurent Daynes
 * @author Doug Simon
 */
public class BytecodeTemplateSource {

    @BYTECODE_TEMPLATE(LOAD_EXCEPTION)
    public static void loadException() {
        JitStackFrameOperation.pushObject(NoninlineTemplateRuntime.loadException());
    }

    @BYTECODE_TEMPLATE(NOP$instrumented$MethodEntry)
    public static void nop(MethodProfile mpo) {
        // entrypoint counters count down to zero ("overflow")
        MethodInstrumentation.recordEntrypoint(mpo);
    }

    @BYTECODE_TEMPLATE(NOP$instrumented$TraceMethod)
    public static void nopTraceMethod(String method) {
        Log.println(method);
    }

    @BYTECODE_TEMPLATE(ACONST_NULL)
    public static void aconst_null() {
        JitStackFrameOperation.pushObject(null);
    }

    @BYTECODE_TEMPLATE(WCONST_0)
    public static void wconst_0() {
        JitStackFrameOperation.pushWord(Address.zero());
    }

    @BYTECODE_TEMPLATE(AALOAD)
    public static void aaload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeReference(0, ArrayAccess.getObject(array, index));
    }

    @BYTECODE_TEMPLATE(AASTORE)
    public static void aastore() {
        int index = JitStackFrameOperation.peekInt(1);
        Object array = JitStackFrameOperation.peekObject(2);
        Object value = JitStackFrameOperation.peekObject(0);
        noninlineArrayStore(index, array, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(ALOAD)
    public static void aload(int dispToLocalSlot) {
        Object value = JitStackFrameOperation.getLocalObject(dispToLocalSlot);
        JitStackFrameOperation.pushObject(value);
    }

    @BYTECODE_TEMPLATE(WLOAD)
    public static void wload(int dispToLocalSlot) {
        Word value = JitStackFrameOperation.getLocalWord(dispToLocalSlot);
        JitStackFrameOperation.pushWord(value);
    }

    @BYTECODE_TEMPLATE(ANEWARRAY)
    public static void anewarray(ResolutionGuard guard) {
        ArrayClassActor arrayClassActor = UnsafeCast.asArrayClassActor(resolveArrayClass(guard));
        int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, CreateReferenceArray.noninlineCreateReferenceArray(arrayClassActor, length));
    }

    @BYTECODE_TEMPLATE(ARETURN)
    public static Object areturn() {
        Object value = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.removeSlots(1);
        return value;
    }

    @BYTECODE_TEMPLATE(WRETURN)
    public static Word wreturn() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        return value;
    }

    @BYTECODE_TEMPLATE(ARRAYLENGTH)
    public static void arraylength() {
        int length = ArrayAccess.readArrayLength(JitStackFrameOperation.peekObject(0));
        JitStackFrameOperation.pokeInt(0, length);
    }

    @BYTECODE_TEMPLATE(ASTORE)
    public static void astore(int displacementToSlot) {
        Object value = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.setLocalObject(displacementToSlot, value);
    }

    @BYTECODE_TEMPLATE(WSTORE)
    public static void wstore(int displacementToSlot) {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.setLocalWord(displacementToSlot, value);
    }

    @BYTECODE_TEMPLATE(ATHROW)
    public static void athrow() {
        Throw.raise(JitStackFrameOperation.peekObject(0));
    }

    @BYTECODE_TEMPLATE(BALOAD)
    public static void baload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getByte(array, index));
    }

    @BYTECODE_TEMPLATE(BASTORE)
    public static void bastore() {
        int index = JitStackFrameOperation.peekInt(1);
        Object array = JitStackFrameOperation.peekObject(2);
        ArrayAccess.checkIndex(array, index);
        byte value = (byte) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setByte(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(BIPUSH)
    public static void bipush(byte value) {
        JitStackFrameOperation.pushInt(value);
    }

    @BYTECODE_TEMPLATE(CALOAD)
    public static void caload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getChar(array, index));
    }

    @BYTECODE_TEMPLATE(CASTORE)
    public static void castore() {
        int index = JitStackFrameOperation.peekInt(1);
        Object array = JitStackFrameOperation.peekObject(2);
        ArrayAccess.checkIndex(array, index);
        char value = (char) JitStackFrameOperation.peekInt(0);
        ArrayAccess.setChar(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(CHECKCAST)
    public static void checkcast(ResolutionGuard guard) {
        resolveAndCheckcast(guard, JitStackFrameOperation.peekObject(0));
    }

    @BYTECODE_TEMPLATE(D2F)
    public static void d2f() {
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, (float) value);
    }

    @BYTECODE_TEMPLATE(D2I)
    public static void d2i() {
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, NoninlineTemplateRuntime.d2i(value));
    }

    @BYTECODE_TEMPLATE(D2L)
    public static void d2l() {
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.pokeLong(0, NoninlineTemplateRuntime.d2l(value));
    }

    @BYTECODE_TEMPLATE(DADD)
    public static void dadd() {
        double value2 = JitStackFrameOperation.peekDouble(0);
        double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(DALOAD)
    public static void daload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.pokeDouble(0, ArrayAccess.getDouble(array, index));
    }

    @BYTECODE_TEMPLATE(DASTORE)
    public static void dastore() {
        int index = JitStackFrameOperation.peekInt(2);
        Object array = JitStackFrameOperation.peekObject(3);
        ArrayAccess.checkIndex(array, index);
        double value = JitStackFrameOperation.peekDouble(0);
        ArrayAccess.setDouble(array, index, value);
        JitStackFrameOperation.removeSlots(4);
    }

    @BYTECODE_TEMPLATE(DCMPG)
    public static void dcmpg() {
        double value2 = JitStackFrameOperation.peekDouble(0);
        double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, JavaBuiltin.DoubleCompareG.doubleCompareG(value1, value2));
    }

    @BYTECODE_TEMPLATE(DCMPL)
    public static void dcmpl() {
        double value2 = JitStackFrameOperation.peekDouble(0);
        double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, JavaBuiltin.DoubleCompareL.doubleCompareL(value1, value2));
    }

    @BYTECODE_TEMPLATE(DCONST)
    public static void dconst(double constant) {
        JitStackFrameOperation.pushDouble(constant);
    }

    @BYTECODE_TEMPLATE(DDIV)
    public static void ddiv() {
        double divisor = JitStackFrameOperation.peekDouble(0);
        double dividend = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, dividend / divisor);
    }

    @BYTECODE_TEMPLATE(DLOAD)
    public static void dload(int displacementToSlot) {
        JitStackFrameOperation.pushDouble(JitStackFrameOperation.getLocalDouble(displacementToSlot));
    }

    @BYTECODE_TEMPLATE(DMUL)
    public static void dmul() {
        double value2 = JitStackFrameOperation.peekDouble(0);
        double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(DNEG)
    public static void dneg(double zero) {
        JitStackFrameOperation.pokeDouble(0, zero - JitStackFrameOperation.peekDouble(0));
    }

    @BYTECODE_TEMPLATE(DREM)
    public static void drem() {
        double value2 = JitStackFrameOperation.peekDouble(0);
        double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(DRETURN)
    public static double dreturn() {
        return JitStackFrameOperation.popDouble();
    }

    @BYTECODE_TEMPLATE(DSTORE)
    public static void dstore(int displacementToSlot) {
        JitStackFrameOperation.setLocalDouble(displacementToSlot, JitStackFrameOperation.popDouble());
    }

    @BYTECODE_TEMPLATE(DSUB)
    public static void dsub() {
        double value2 = JitStackFrameOperation.peekDouble(0);
        double value1 = JitStackFrameOperation.peekDouble(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeDouble(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(DUP)
    public static void dup() {
        JitStackFrameOperation.pushWord(JitStackFrameOperation.peekWord(0));
    }

    @BYTECODE_TEMPLATE(DUP_X1)
    public static void dup_x1() {
        Word value1 = JitStackFrameOperation.peekWord(0);
        Word value2 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pokeWord(1, value1);
        JitStackFrameOperation.pokeWord(0, value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @BYTECODE_TEMPLATE(DUP_X2)
    public static void dup_x2() {
        Word value1 = JitStackFrameOperation.peekWord(0);
        Word value2 = JitStackFrameOperation.peekWord(1);
        Word value3 = JitStackFrameOperation.peekWord(2);
        JitStackFrameOperation.pushWord(value1);
        JitStackFrameOperation.pokeWord(1, value2);
        JitStackFrameOperation.pokeWord(2, value3);
        JitStackFrameOperation.pokeWord(3, value1);
    }

    @BYTECODE_TEMPLATE(DUP2)
    public static void dup2() {
        Word value1 = JitStackFrameOperation.peekWord(0);
        Word value2 = JitStackFrameOperation.peekWord(1);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @BYTECODE_TEMPLATE(DUP2_X1)
    public static void dup2_x1() {
        Word value1 = JitStackFrameOperation.peekWord(0);
        Word value2 = JitStackFrameOperation.peekWord(1);
        Word value3 = JitStackFrameOperation.peekWord(2);
        JitStackFrameOperation.pokeWord(2, value2);
        JitStackFrameOperation.pokeWord(1, value1);
        JitStackFrameOperation.pokeWord(0, value3);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @BYTECODE_TEMPLATE(DUP2_X2)
    public static void dup2_x2() {
        Word value1 = JitStackFrameOperation.peekWord(0);
        Word value2 = JitStackFrameOperation.peekWord(1);
        Word value3 = JitStackFrameOperation.peekWord(2);
        Word value4 = JitStackFrameOperation.peekWord(3);
        JitStackFrameOperation.pokeWord(3, value2);
        JitStackFrameOperation.pokeWord(2, value1);
        JitStackFrameOperation.pokeWord(1, value4);
        JitStackFrameOperation.pokeWord(0, value3);
        JitStackFrameOperation.pushWord(value2);
        JitStackFrameOperation.pushWord(value1);
    }

    @BYTECODE_TEMPLATE(F2D)
    public static void f2d() {
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @BYTECODE_TEMPLATE(F2I)
    public static void f2i() {
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.pokeInt(0, NoninlineTemplateRuntime.f2i(value));
    }

    @BYTECODE_TEMPLATE(F2L)
    public static void f2l() {
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, NoninlineTemplateRuntime.f2l(value));
    }

    @BYTECODE_TEMPLATE(FADD)
    public static void fadd() {
        float value2 = JitStackFrameOperation.peekFloat(0);
        float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(FALOAD)
    public static void faload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, ArrayAccess.getFloat(array, index));
    }

    @BYTECODE_TEMPLATE(FASTORE)
    public static void fastore() {
        int index = JitStackFrameOperation.peekInt(1);
        Object array = JitStackFrameOperation.peekObject(2);
        ArrayAccess.checkIndex(array, index);
        float value = JitStackFrameOperation.peekFloat(0);
        ArrayAccess.setFloat(array, index, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(FCMPG)
    public static void fcmpg() {
        float value2 = JitStackFrameOperation.peekFloat(0);
        float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        int result = JavaBuiltin.FloatCompareG.floatCompareG(value1, value2);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @BYTECODE_TEMPLATE(FCMPL)
    public static void fcmpl() {
        float value2 = JitStackFrameOperation.peekFloat(0);
        float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        int result = JavaBuiltin.FloatCompareL.floatCompareL(value1, value2);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @BYTECODE_TEMPLATE(FCONST)
    public static void fconst(float constant) {
        JitStackFrameOperation.pushFloat(constant);
    }

    @BYTECODE_TEMPLATE(FDIV)
    public static void fdiv() {
        float divisor = JitStackFrameOperation.peekFloat(0);
        float dividend = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, dividend / divisor);
    }

    @BYTECODE_TEMPLATE(FLOAD)
    public static void fload(int dispToLocalSlot) {
        float value = JitStackFrameOperation.getLocalFloat(dispToLocalSlot);
        JitStackFrameOperation.pushFloat(value);
    }

    @BYTECODE_TEMPLATE(FMUL)
    public static void fmul() {
        float value2 = JitStackFrameOperation.peekFloat(0);
        float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(FNEG)
    public static void fneg(float zero) {
        JitStackFrameOperation.pokeFloat(0, zero - JitStackFrameOperation.peekFloat(0));
    }

    @BYTECODE_TEMPLATE(FREM)
    public static void frem() {
        float value2 = JitStackFrameOperation.peekFloat(0);
        float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(FRETURN)
    public static float freturn() {
        return JitStackFrameOperation.popFloat();
    }

    @BYTECODE_TEMPLATE(FSTORE)
    public static void fstore(int displacementToSlot) {
        JitStackFrameOperation.setLocalFloat(displacementToSlot, JitStackFrameOperation.popFloat());
    }

    @BYTECODE_TEMPLATE(FSUB)
    public static void fsub() {
        float value2 = JitStackFrameOperation.peekFloat(0);
        float value1 = JitStackFrameOperation.peekFloat(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(GETFIELD$reference)
    public static void getfieldReference(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeReference(0, resolveAndGetFieldReference(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$word)
    public static void getfieldWord(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeWord(0, resolveAndGetFieldWord(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$byte)
    public static void getfieldByte(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldByte(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$char)
    public static void getfieldChar(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldChar(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$double)
    public static void getfieldDouble(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, resolveAndGetFieldDouble(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$float)
    public static void getfieldFloat(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeFloat(0, resolveAndGetFieldFloat(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$int)
    public static void getfieldInt(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldInt(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$long)
    public static void getfieldLong(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, resolveAndGetFieldLong(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$short)
    public static void getfieldShort(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, resolveAndGetFieldShort(guard, object));
    }

    @BYTECODE_TEMPLATE(GETFIELD$boolean)
    public static void getfieldBoolean(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(resolveAndGetFieldBoolean(guard, object)));
    }

    @BYTECODE_TEMPLATE(PUTFIELD$reference)
    public static void putfieldReference(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        Object value = JitStackFrameOperation.peekObject(0);
        resolveAndPutFieldReference(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$word)
    public static void putfieldWord(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        Word value = JitStackFrameOperation.peekWord(0);
        resolveAndPutFieldWord(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$byte)
    public static void putfieldByte(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        byte value = (byte) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldByte(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$char)
    public static void putfieldChar(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        char value = (char) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldChar(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$double)
    public static void putfieldDouble(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(2);
        double value = JitStackFrameOperation.peekDouble(0);
        resolveAndPutFieldDouble(guard, object, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$float)
    public static void putfieldFloat(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        float value = JitStackFrameOperation.peekFloat(0);
        resolveAndPutFieldFloat(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$int)
    public static void putfieldInt(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        int value = JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldInt(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$long)
    public static void putfieldLong(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(2);
        long value = JitStackFrameOperation.peekLong(0);
        resolveAndPutFieldLong(guard, object, value);
        JitStackFrameOperation.removeSlots(3);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$short)
    public static void putfieldShort(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        short value = (short) JitStackFrameOperation.peekInt(0);
        resolveAndPutFieldShort(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(PUTFIELD$boolean)
    public static void putfieldBoolean(ResolutionGuard.InPool guard) {
        Object object = JitStackFrameOperation.peekObject(1);
        boolean value = UnsafeCast.asBoolean((byte) JitStackFrameOperation.peekInt(0));
        resolveAndPutFieldBoolean(guard, object, value);
        JitStackFrameOperation.removeSlots(2);
    }

    @BYTECODE_TEMPLATE(GETSTATIC$byte)
    public static void getstaticByte(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticByte(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$char)
    public static void getstaticChar(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticChar(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$double)
    public static void getstaticDouble(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushDouble(resolveAndGetStaticDouble(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$float)
    public static void getstaticFloat(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushFloat(resolveAndGetStaticFloat(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$int)
    public static void getstaticInt(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticInt(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$long)
    public static void getstaticLong(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushLong(resolveAndGetStaticLong(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$reference)
    public static void getstaticReference(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushObject(resolveAndGetStaticReference(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$word)
    public static void getstaticWord(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushWord(resolveAndGetStaticWord(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$short)
    public static void getstaticShort(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushInt(resolveAndGetStaticShort(guard));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$boolean)
    public static void getstaticBoolean(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.pushInt(UnsafeCast.asByte(resolveAndGetStaticBoolean(guard)));
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$byte)
    public static void putstaticByte(ResolutionGuard.InPool guard) {
        resolveAndPutStaticByte(guard, (byte) JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$char)
    public static void putstaticChar(ResolutionGuard.InPool guard) {
        resolveAndPutStaticChar(guard, (char) JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$double)
    public static void putstaticDouble(ResolutionGuard.InPool guard) {
        resolveAndPutStaticDouble(guard, JitStackFrameOperation.popDouble());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$float)
    public static void putstaticFloat(ResolutionGuard.InPool guard) {
        resolveAndPutStaticFloat(guard, JitStackFrameOperation.popFloat());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$int)
    public static void putstaticInt(ResolutionGuard.InPool guard) {
        resolveAndPutStaticInt(guard, JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$long)
    public static void putstaticLong(ResolutionGuard.InPool guard) {
        resolveAndPutStaticLong(guard, JitStackFrameOperation.popLong());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$reference)
    public static void putstaticReference(ResolutionGuard.InPool guard) {
        resolveAndPutStaticReference(guard, JitStackFrameOperation.peekObject(0));
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$word)
    public static void putstaticWord(ResolutionGuard.InPool guard) {
        resolveAndPutStaticWord(guard, JitStackFrameOperation.peekWord(0));
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$short)
    public static void putstaticShort(ResolutionGuard.InPool guard) {
        resolveAndPutStaticShort(guard, (short) JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$boolean)
    public static void putstaticBoolean(ResolutionGuard.InPool guard) {
        resolveAndPutStaticBoolean(guard, UnsafeCast.asBoolean((byte) JitStackFrameOperation.popInt()));
    }

    @BYTECODE_TEMPLATE(I2B)
    public static void i2b() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (byte) value);
    }

    @BYTECODE_TEMPLATE(I2C)
    public static void i2c() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (char) value);
    }

    @BYTECODE_TEMPLATE(I2F)
    public static void i2f() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeFloat(0, value);
    }

    @BYTECODE_TEMPLATE(I2S)
    public static void i2s() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeInt(0, (short) value);
    }

    @BYTECODE_TEMPLATE(I2L)
    public static void i2l() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, value);
    }

    @BYTECODE_TEMPLATE(I2D)
    public static void i2d() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @BYTECODE_TEMPLATE(IADD)
    public static void iadd() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(IALOAD)
    public static void iaload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getInt(array, index));
    }

    @BYTECODE_TEMPLATE(IAND)
    public static void iand() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 & value2);
    }

    @BYTECODE_TEMPLATE(IASTORE)
    public static void iastore() {
        int index = JitStackFrameOperation.peekInt(1);
        Object array = JitStackFrameOperation.peekObject(2);
        ArrayAccess.checkIndex(array, index);
        int value = JitStackFrameOperation.peekInt(0);
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
        int divisor = JitStackFrameOperation.peekInt(0);
        int dividend = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, dividend / divisor);
    }

    @BYTECODE_TEMPLATE(WDIV)
    public static void wdiv() {
        Address divisor = JitStackFrameOperation.peekWord(0).asAddress();
        Address dividend = JitStackFrameOperation.peekWord(1).asAddress();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeWord(0, dividend.dividedBy(divisor));
    }

    @BYTECODE_TEMPLATE(WDIVI)
    public static void wdivi() {
        int divisor = JitStackFrameOperation.peekInt(0);
        Address dividend = JitStackFrameOperation.peekWord(1).asAddress();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeWord(0, dividend.dividedBy(divisor));
    }

    @BYTECODE_TEMPLATE(IINC)
    public static void iinc(int dispToLocalSlot, int increment) {
        JitStackFrameOperation.setLocalInt(dispToLocalSlot, JitStackFrameOperation.getLocalInt(dispToLocalSlot) + increment);
    }

    @BYTECODE_TEMPLATE(ILOAD)
    public static void iload(int dispToLocalSlot) {
        int value = JitStackFrameOperation.getLocalInt(dispToLocalSlot);
        JitStackFrameOperation.pushInt(value);
    }

    @BYTECODE_TEMPLATE(IMUL)
    public static void imul() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(INEG)
    public static void ineg() {
        JitStackFrameOperation.pokeInt(0, -JitStackFrameOperation.peekInt(0));
    }

    @BYTECODE_TEMPLATE(INSTANCEOF)
    public static void instanceof_(ResolutionGuard guard) {
        ClassActor classActor = UnsafeCast.asClassActor(ResolveClass.resolveClass(guard));
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$void)
    public static void invokevirtualVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$float)
    public static void invokevirtualFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$long)
    public static void invokevirtualLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$double)
    public static void invokevirtualDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$word)
    public static void invokevirtualWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$void)
    public static void invokeinterfaceVoid(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$float)
    public static void invokeinterfaceFloat(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$long)
    public static void invokeinterfaceLong(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$double)
    public static void invokeinterfaceDouble(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$word)
    public static void invokeinterfaceWord(ResolutionGuard.InPool guard, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$void)
    public static void invokespecialVoid(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallVoid(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$float)
    public static void invokespecialFloat(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallFloat(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$long)
    public static void invokespecialLong(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallLong(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$double)
    public static void invokespecialDouble(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallDouble(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESPECIAL$word)
    public static void invokespecialWord(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallWord(resolveSpecialMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$void)
    public static void invokestaticVoid(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallVoid(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$float)
    public static void invokestaticFloat(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallFloat(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$long)
    public static void invokestaticLong(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallLong(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$double)
    public static void invokestaticDouble(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallDouble(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(INVOKESTATIC$word)
    public static void invokestaticWord(ResolutionGuard.InPool guard) {
        JitStackFrameOperation.indirectCallWord(resolveStaticMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(IOR)
    public static void ior() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 | value2);
    }

    @BYTECODE_TEMPLATE(IREM)
    public static void irem() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(WREM)
    public static void wrem() {
        Address divisor = JitStackFrameOperation.peekWord(0).asAddress();
        Address dividend = JitStackFrameOperation.peekWord(1).asAddress();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeWord(0, dividend.remainder(divisor));
    }

    @BYTECODE_TEMPLATE(WREMI)
    public static void wremi() {
        int divisor = JitStackFrameOperation.peekInt(0);
        Address dividend = JitStackFrameOperation.peekWord(1).asAddress();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, dividend.remainder(divisor));
    }

    @BYTECODE_TEMPLATE(IRETURN)
    public static int ireturn() {
        return JitStackFrameOperation.popInt();
    }

    @BYTECODE_TEMPLATE(ISHL)
    public static void ishl() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 << value2);
    }

    @BYTECODE_TEMPLATE(ISHR)
    public static void ishr() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 >> value2);
    }

    @BYTECODE_TEMPLATE(ISTORE)
    public static void istore(int displacementToSlot) {
        JitStackFrameOperation.setLocalInt(displacementToSlot, JitStackFrameOperation.popInt());
    }

    @BYTECODE_TEMPLATE(ISUB)
    public static void isub() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(IUSHR)
    public static void iushr() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, value1 >>> value2);
    }

    @BYTECODE_TEMPLATE(IXOR)
    public static void ixor() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
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
        ClassActor classActor = ResolveClass.resolveClass(guard);
        Object mirror = NoninlineTemplateRuntime.getClassMirror(classActor);
        JitStackFrameOperation.pushObject(mirror);
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
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.pokeDouble(0, value);
    }

    @BYTECODE_TEMPLATE(L2F)
    public static void l2f() {
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, value);
    }

    @BYTECODE_TEMPLATE(L2I)
    public static void l2i() {
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, (int) value);
    }

    @BYTECODE_TEMPLATE(LADD)
    public static void ladd() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 + value2);
    }

    @BYTECODE_TEMPLATE(LALOAD)
    public static void laload() {
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.pokeLong(0, ArrayAccess.getLong(array, index));
    }

    @BYTECODE_TEMPLATE(LAND)
    public static void land() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 & value2);
    }

    @BYTECODE_TEMPLATE(LASTORE)
    public static void lastore() {
        int index = JitStackFrameOperation.peekInt(2);
        Object array = JitStackFrameOperation.peekObject(3);
        ArrayAccess.checkIndex(array, index);
        long value = JitStackFrameOperation.peekLong(0);
        ArrayAccess.setLong(array, index, value);
        JitStackFrameOperation.removeSlots(4);
    }

    @BYTECODE_TEMPLATE(LCMP)
    public static void lcmp() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        int result = JavaBuiltin.LongCompare.longCompare(value1, value2);
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, result);
    }

    @BYTECODE_TEMPLATE(LCONST)
    public static void lconst(long constant) {
        JitStackFrameOperation.pushLong(constant);
    }

    @BYTECODE_TEMPLATE(LLOAD)
    public static void lload(int displacementToSlot) {
        JitStackFrameOperation.pushLong(JitStackFrameOperation.getLocalLong(displacementToSlot));
    }

    @BYTECODE_TEMPLATE(LDIV)
    public static void ldiv() {
        long divisor = JitStackFrameOperation.peekLong(0);
        long dividend = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, dividend / divisor);
    }

    @BYTECODE_TEMPLATE(LMUL)
    public static void lmul() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 * value2);
    }

    @BYTECODE_TEMPLATE(LNEG)
    public static void lneg() {
        JitStackFrameOperation.pokeLong(0, -JitStackFrameOperation.peekLong(0));
    }

    @BYTECODE_TEMPLATE(LOR)
    public static void lor() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 | value2);
    }

    @BYTECODE_TEMPLATE(LREM)
    public static void lrem() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 % value2);
    }

    @BYTECODE_TEMPLATE(LRETURN)
    public static long lreturn() {
        return JitStackFrameOperation.popLong();
    }

    @BYTECODE_TEMPLATE(LSHL)
    public static void lshl() {
        int value2 = JitStackFrameOperation.peekInt(0);
        long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 << value2);
    }

    @BYTECODE_TEMPLATE(LSHR)
    public static void lshr() {
        int value2 = JitStackFrameOperation.peekInt(0);
        long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 >> value2);
    }

    @BYTECODE_TEMPLATE(LSTORE)
    public static void lstor(int displacementToSlot) {
        JitStackFrameOperation.setLocalLong(displacementToSlot, JitStackFrameOperation.popLong());
    }

    @BYTECODE_TEMPLATE(LSUB)
    public static void lsub() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 - value2);
    }

    @BYTECODE_TEMPLATE(LUSHR)
    public static void lushr() {
        int value2 = JitStackFrameOperation.peekInt(0);
        long value1 = JitStackFrameOperation.peekLong(1);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, value1 >>> value2);
    }

    @BYTECODE_TEMPLATE(LXOR)
    public static void lxor() {
        long value2 = JitStackFrameOperation.peekLong(0);
        long value1 = JitStackFrameOperation.peekLong(2);
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeLong(0, value1 ^ value2);
    }

    @BYTECODE_TEMPLATE(MONITORENTER)
    public static void monitorenter() {
        Object object = JitStackFrameOperation.peekObject(0);
        Monitor.noninlineEnter(object);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(MONITOREXIT)
    public static void monitorexit() {
        Object object = JitStackFrameOperation.peekObject(0);
        Monitor.noninlineExit(object);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(MULTIANEWARRAY)
    public static void multianewarray(ResolutionGuard guard, int[] lengthsShared) {
        ClassActor arrayClassActor = ResolveClass.resolveClass(guard);

        // Need to use an unsafe cast to remove the checkcast inserted by javac as that causes this
        // template to have a reference literal in its compiled form.
        int[] lengths = UnsafeCast.asIntArray(lengthsShared.clone());
        int numberOfDimensions = lengths.length;

        for (int i = 1; i <= numberOfDimensions; i++) {
            int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushObject(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }

    @BYTECODE_TEMPLATE(NEW)
    public static void new_(ResolutionGuard guard) {
        JitStackFrameOperation.pushObject(resolveClassForNewAndCreate(guard));
    }

    @BYTECODE_TEMPLATE(NEWARRAY)
    public static void newarray(Kind kind) {
        int length = JitStackFrameOperation.peekInt(0);
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
        int index = JitStackFrameOperation.peekInt(0);
        Object array = JitStackFrameOperation.peekObject(1);
        ArrayAccess.checkIndex(array, index);
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ArrayAccess.getShort(array, index));
    }

    @BYTECODE_TEMPLATE(SASTORE)
    public static void sastore() {
        short value = (short) JitStackFrameOperation.peekInt(0);
        int index = JitStackFrameOperation.peekInt(1);
        Object array = JitStackFrameOperation.peekObject(2);
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
        Word value0 = JitStackFrameOperation.peekWord(0);
        Word value1 = JitStackFrameOperation.peekWord(1);
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
        Object value = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.removeSlots(1);
        SpecialBuiltin.compareWords(toWord(value), Address.zero());
    }

    @INLINE
    private static void icmp_prefix() {
        int value2 = JitStackFrameOperation.peekInt(0);
        int value1 = JitStackFrameOperation.peekInt(1);
        JitStackFrameOperation.removeSlots(2);
        SpecialBuiltin.compareInts(value1, value2);
    }

    @INTRINSIC(Bytecodes.UNSAFE_CAST)
    private static native Word toWord(Object object);

    @INLINE
    private static void acmp_prefix() {
        Object value2 = JitStackFrameOperation.peekObject(0);
        Object value1 = JitStackFrameOperation.peekObject(1);
        JitStackFrameOperation.removeSlots(2);
        SpecialBuiltin.compareWords(toWord(value1), toWord(value2));
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
        JitStackFrameOperation.pushObject(NoninlineTemplateRuntime.noninlineNew(classActor));
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
        JitStackFrameOperation.pushObject(TupleAccess.readObject(staticTuple, offset));
    }

    @BYTECODE_TEMPLATE(GETSTATIC$word$init)
    public static void getstaticWord(Object staticTuple, int offset) {
        JitStackFrameOperation.pushWord(TupleAccess.readWord(staticTuple, offset));
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
        byte value = (byte) JitStackFrameOperation.popInt();
        TupleAccess.writeByte(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$char$init)
    public static void putstaticChar(Object staticTuple, int offset) {
        char value = (char) JitStackFrameOperation.popInt();
        TupleAccess.writeChar(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$double$init)
    public static void putstaticDouble(Object staticTuple, int offset) {
        double value = JitStackFrameOperation.popDouble();
        TupleAccess.writeDouble(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$float$init)
    public static void putstaticFloat(Object staticTuple, int offset) {
        float value = JitStackFrameOperation.popFloat();
        TupleAccess.writeFloat(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$int$init)
    public static void putstaticInt(Object staticTuple, int offset) {
        int value = JitStackFrameOperation.popInt();
        TupleAccess.writeInt(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$long$init)
    public static void putstaticLong(Object staticTuple, int offset) {
        long value = JitStackFrameOperation.popLong();
        TupleAccess.writeLong(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$reference$init)
    public static void putstaticReference(Object staticTuple, int offset) {
        Object value = JitStackFrameOperation.peekObject(0);
        TupleAccess.noninlineWriteObject(staticTuple, offset, value);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$word$init)
    public static void putstaticWord(Object staticTuple, int offset) {
        Word value = JitStackFrameOperation.peekWord(0);
        TupleAccess.writeWord(staticTuple, offset, value);
        JitStackFrameOperation.removeSlots(1);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$short$init)
    public static void putstaticShort(Object staticTuple, int offset) {
        short value = (short) JitStackFrameOperation.popInt();
        TupleAccess.writeShort(staticTuple, offset, value);
    }

    @BYTECODE_TEMPLATE(PUTSTATIC$boolean$init)
    public static void putstaticBoolean(Object staticTuple, int offset) {
        boolean value = UnsafeCast.asBoolean((byte) JitStackFrameOperation.popInt());
        TupleAccess.writeBoolean(staticTuple, offset, value);
    }

    // Instructions with a resolved class operand

    @BYTECODE_TEMPLATE(LDC$reference$resolved)
    public static void rldc(Object value) {
        JitStackFrameOperation.pushObject(value);
    }

    @BYTECODE_TEMPLATE(CHECKCAST$resolved)
    public static void checkcast(ClassActor classActor) {
        Snippet.CheckCast.checkCast(classActor, JitStackFrameOperation.peekObject(0));
    }

    @BYTECODE_TEMPLATE(INSTANCEOF$resolved)
    public static void instanceof_(ClassActor classActor) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(Snippet.InstanceOf.instanceOf(classActor, object)));
    }

    @BYTECODE_TEMPLATE(ANEWARRAY$resolved)
    public static void anewarray(ArrayClassActor arrayClassActor) {
        int length = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeReference(0, CreateReferenceArray.noninlineCreateReferenceArray(arrayClassActor, length));
    }

    @BYTECODE_TEMPLATE(MULTIANEWARRAY$resolved)
    public static void multianewarray(ArrayClassActor arrayClassActor, int[] lengthsShared) {
        // Need to use an unsafe cast to remove the checkcast inserted by javac as that causes this
        // template to have a reference literal in its compiled form.
        int[] lengths = UnsafeCast.asIntArray(lengthsShared.clone());
        int numberOfDimensions = lengths.length;
        for (int i = 1; i <= numberOfDimensions; i++) {
            int length = JitStackFrameOperation.popInt();
            Snippet.CheckArrayDimension.checkArrayDimension(length);
            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);
        }
        JitStackFrameOperation.pushObject(CreateMultiReferenceArray.createMultiReferenceArray(arrayClassActor, lengths));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$reference$resolved)
    public static void getfieldReference(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeReference(0, TupleAccess.readObject(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$word$resolved)
    public static void getfieldWord(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeWord(0, TupleAccess.readWord(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$byte$resolved)
    public static void getfieldByte(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readByte(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$char$resolved)
    public static void getfieldChar(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readChar(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$double$resolved)
    public static void getfieldDouble(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeDouble(0, TupleAccess.readDouble(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$float$resolved)
    public static void getfieldFloat(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeFloat(0, TupleAccess.readFloat(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$int$resolved)
    public static void getfieldInt(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readInt(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$long$resolved)
    public static void getfieldLong(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.addSlots(1);
        JitStackFrameOperation.pokeLong(0, TupleAccess.readLong(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$short$resolved)
    public static void getfieldShort(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, TupleAccess.readShort(object, offset));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.GETFIELD$boolean$resolved)
    public static void getfieldBoolean(int offset) {
        Object object = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.pokeInt(0, UnsafeCast.asByte(TupleAccess.readBoolean(object, offset)));
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$reference$resolved)
    public static void putfieldReference(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        Object value = JitStackFrameOperation.peekObject(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.noninlineWriteObject(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$word$resolved)
    public static void putfieldWord(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeWord(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$byte$resolved)
    public static void putfieldByte(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        byte value = (byte) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeByte(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$char$resolved)
    public static void putfieldChar(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        char value = (char) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeChar(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$double$resolved)
    public static void putfieldDouble(int offset) {
        Object object = JitStackFrameOperation.peekObject(2);
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(3);
        TupleAccess.writeDouble(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$float$resolved)
    public static void putfieldFloat(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeFloat(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$int$resolved)
    public static void putfieldInt(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeInt(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$long$resolved)
    public static void putfieldLong(int offset) {
        Object object = JitStackFrameOperation.peekObject(2);
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(3);
        TupleAccess.writeLong(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$short$resolved)
    public static void putfieldShort(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        short value = (short) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(2);
        TupleAccess.writeShort(object, offset, value);
    }

    @BYTECODE_TEMPLATE(BytecodeTemplate.PUTFIELD$boolean$resolved)
    public static void putfieldBoolean(int offset) {
        Object object = JitStackFrameOperation.peekObject(1);
        boolean value = UnsafeCast.asBoolean((byte) JitStackFrameOperation.peekInt(0));
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
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$float$resolved)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$long$resolved)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$double$resolved)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$word$resolved)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();
        JitStackFrameOperation.indirectCallWord(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$void$resolved)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallVoid(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$float$resolved)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallFloat(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$long$resolved)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallLong(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$double$resolved)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        JitStackFrameOperation.indirectCallDouble(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    @BYTECODE_TEMPLATE(INVOKEINTERFACE$word$resolved)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
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
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$float$instrumented)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$long$instrumented)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$double$instrumented)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEVIRTUAL$word$instrumented)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$void$instrumented)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallVoid(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$float$instrumented)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallFloat(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$long$instrumented)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallLong(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$double$instrumented)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallDouble(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @BYTECODE_TEMPLATE(INVOKEINTERFACE$word$instrumented)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex, MethodProfile mpo, int mpoIndex) {
        Object receiver = JitStackFrameOperation.peekObject(receiverStackIndex);
        Address entryPoint = selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);
        JitStackFrameOperation.indirectCallWord(entryPoint, VTABLE_ENTRY_POINT, receiver);
    }

    @INLINE
    private static Address selectVirtualMethod(Object receiver, int vTableIndex, MethodProfile mpo, int mpoIndex) {
        Hub hub = ObjectAccess.readHub(receiver);
        Address entryPoint = hub.getWord(vTableIndex).asAddress();
        MethodInstrumentation.recordType(mpo, hub, mpoIndex, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES);
        return entryPoint;
    }

    @INLINE
    private static Address selectInterfaceMethod(Object receiver, InterfaceMethodActor interfaceMethodActor, MethodProfile mpo, int mpoIndex) {
        Hub hub = ObjectAccess.readHub(receiver);
        Address entryPoint = SelectInterfaceMethod.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();
        MethodInstrumentation.recordType(mpo, hub, mpoIndex, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES);
        return entryPoint;
    }

    @BYTECODE_TEMPLATE(PREAD_BYTE)
    public static void pread_byte() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readByte(off));
    }

    @BYTECODE_TEMPLATE(PREAD_CHAR)
    public static void pread_char() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readChar(off));
    }

    @BYTECODE_TEMPLATE(PREAD_SHORT)
    public static void pread_short() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readShort(off));
    }

    @BYTECODE_TEMPLATE(PREAD_INT)
    public static void pread_int() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readInt(off));
    }

    @BYTECODE_TEMPLATE(PREAD_FLOAT)
    public static void pread_float() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, ptr.readFloat(off));
    }

    @BYTECODE_TEMPLATE(PREAD_LONG)
    public static void pread_long() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.pokeLong(0, ptr.readLong(off));
    }

    @BYTECODE_TEMPLATE(PREAD_DOUBLE)
    public static void pread_double() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.pokeDouble(0, ptr.readDouble(off));
    }

    @BYTECODE_TEMPLATE(PREAD_WORD)
    public static void pread_word() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeWord(0, ptr.readWord(off));
    }

    @BYTECODE_TEMPLATE(PREAD_REFERENCE)
    public static void pread_reference() {
        Offset off = JitStackFrameOperation.peekWord(0).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeReference(0, ptr.readReference(off));
    }

    @BYTECODE_TEMPLATE(PREAD_BYTE_I)
    public static void pread_byte_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readByte(off));
    }

    @BYTECODE_TEMPLATE(PREAD_CHAR_I)
    public static void pread_char_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readChar(off));
    }

    @BYTECODE_TEMPLATE(PREAD_SHORT_I)
    public static void pread_short_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readShort(off));
    }

    @BYTECODE_TEMPLATE(PREAD_INT_I)
    public static void pread_int_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeInt(0, ptr.readInt(off));
    }

    @BYTECODE_TEMPLATE(PREAD_FLOAT_I)
    public static void pread_float_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeFloat(0, ptr.readFloat(off));
    }

    @BYTECODE_TEMPLATE(PREAD_LONG_I)
    public static void pread_long_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.pokeLong(0, ptr.readLong(off));
    }

    @BYTECODE_TEMPLATE(PREAD_DOUBLE_I)
    public static void pread_double_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.pokeDouble(0, ptr.readDouble(off));
    }

    @BYTECODE_TEMPLATE(PREAD_WORD_I)
    public static void pread_word_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeWord(0, ptr.readWord(off));
    }

    @BYTECODE_TEMPLATE(PREAD_REFERENCE_I)
    public static void pread_reference_i() {
        int off = JitStackFrameOperation.peekInt(0);
        Pointer ptr = JitStackFrameOperation.peekWord(1).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeReference(0, ptr.readReference(off));
    }

    @BYTECODE_TEMPLATE(PWRITE_BYTE)
    public static void pwrite_byte() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        Offset off = JitStackFrameOperation.peekWord(1).asOffset();
        byte value = (byte) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeByte(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_SHORT)
    public static void pwrite_short() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        Offset off = JitStackFrameOperation.peekWord(1).asOffset();
        short value = (short) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeShort(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_INT)
    public static void pwrite_int() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        Offset off = JitStackFrameOperation.peekWord(1).asOffset();
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeInt(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_FLOAT)
    public static void pwrite_float() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        Offset off = JitStackFrameOperation.peekWord(1).asOffset();
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeFloat(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_LONG)
    public static void pwrite_long() {
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        Offset off = JitStackFrameOperation.peekWord(2).asOffset();
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(4);
        ptr.writeLong(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_DOUBLE)
    public static void pwrite_double() {
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        Offset off = JitStackFrameOperation.peekWord(2).asOffset();
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(4);
        ptr.writeDouble(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_WORD)
    public static void pwrite_word() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        Offset off = JitStackFrameOperation.peekWord(1).asOffset();
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeWord(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_REFERENCE)
    public static void pwrite_reference() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        Offset off = JitStackFrameOperation.peekWord(1).asOffset();
        Reference value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeReference(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_BYTE_I)
    public static void pwrite_byte_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        int off = JitStackFrameOperation.peekInt(1);
        byte value = (byte) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeByte(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_SHORT_I)
    public static void pwrite_short_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        int off = JitStackFrameOperation.peekInt(1);
        short value = (short) JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeShort(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_INT_I)
    public static void pwrite_int_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        int off = JitStackFrameOperation.peekInt(1);
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeInt(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_FLOAT_I)
    public static void pwrite_float_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        int off = JitStackFrameOperation.peekInt(1);
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeFloat(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_LONG_I)
    public static void pwrite_long_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        int off = JitStackFrameOperation.peekInt(2);
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.removeSlots(4);
        ptr.writeLong(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_DOUBLE_I)
    public static void pwrite_double_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        int off = JitStackFrameOperation.peekInt(2);
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.removeSlots(4);
        ptr.writeDouble(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_WORD_I)
    public static void pwrite_word_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        int off = JitStackFrameOperation.peekInt(1);
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeWord(off, value);
    }

    @BYTECODE_TEMPLATE(PWRITE_REFERENCE_I)
    public static void pwrite_reference_i() {
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        int off = JitStackFrameOperation.peekInt(1);
        Reference value = JitStackFrameOperation.peekReference(0);
        JitStackFrameOperation.removeSlots(3);
        ptr.writeReference(off, value);
    }

    @BYTECODE_TEMPLATE(PGET_BYTE)
    public static void pget_byte() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeInt(0, ptr.getByte(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_SHORT)
    public static void pget_short() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeInt(0, ptr.getShort(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_CHAR)
    public static void pget_char() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeInt(0, ptr.getChar(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_INT)
    public static void pget_int() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeInt(0, ptr.getInt(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_FLOAT)
    public static void pget_float() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeFloat(0, ptr.getFloat(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_LONG)
    public static void pget_long() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeLong(0, ptr.getLong(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_DOUBLE)
    public static void pget_double() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(1);
        JitStackFrameOperation.pokeDouble(0, ptr.getDouble(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_WORD)
    public static void pget_word() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeWord(0, ptr.getWord(disp, index));
    }

    @BYTECODE_TEMPLATE(PGET_REFERENCE)
    public static void pget_reference() {
        int index = JitStackFrameOperation.peekInt(0);
        int disp = JitStackFrameOperation.peekInt(1);
        Pointer ptr = JitStackFrameOperation.peekWord(2).asPointer();
        JitStackFrameOperation.removeSlots(2);
        JitStackFrameOperation.pokeReference(0, ptr.getReference(disp, index));
    }

    @BYTECODE_TEMPLATE(PSET_BYTE)
    public static void pset_byte() {
        byte value = (byte) JitStackFrameOperation.peekInt(0);
        int index = JitStackFrameOperation.peekInt(1);
        int disp = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(4);
        ptr.setByte(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_SHORT)
    public static void pset_short() {
        short value = (short) JitStackFrameOperation.peekInt(0);
        int index = JitStackFrameOperation.peekInt(1);
        int disp = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(4);
        ptr.setShort(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_INT)
    public static void pset_int() {
        int value = JitStackFrameOperation.peekInt(0);
        int index = JitStackFrameOperation.peekInt(1);
        int disp = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(4);
        ptr.setInt(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_FLOAT)
    public static void pset_float() {
        float value = JitStackFrameOperation.peekFloat(0);
        int index = JitStackFrameOperation.peekInt(1);
        int disp = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(4);
        ptr.setFloat(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_LONG)
    public static void pset_long() {
        long value = JitStackFrameOperation.peekLong(0);
        int index = JitStackFrameOperation.peekInt(2);
        int disp = JitStackFrameOperation.peekInt(3);
        Pointer ptr = JitStackFrameOperation.peekWord(4).asPointer();
        JitStackFrameOperation.removeSlots(5);
        ptr.setLong(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_DOUBLE)
    public static void pset_double() {
        double value = JitStackFrameOperation.peekDouble(0);
        int index = JitStackFrameOperation.peekInt(2);
        int disp = JitStackFrameOperation.peekInt(3);
        Pointer ptr = JitStackFrameOperation.peekWord(4).asPointer();
        JitStackFrameOperation.removeSlots(5);
        ptr.setDouble(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_WORD)
    public static void pset_word() {
        Word value = JitStackFrameOperation.peekWord(0);
        int index = JitStackFrameOperation.peekInt(1);
        int disp = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(4);
        ptr.setWord(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PSET_REFERENCE)
    public static void pset_reference() {
        Reference value = JitStackFrameOperation.peekReference(0);
        int index = JitStackFrameOperation.peekInt(1);
        int disp = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(4);
        ptr.setReference(disp, index, value);
    }

    @BYTECODE_TEMPLATE(PCMPSWP_INT)
    public static void pcmpswp_int() {
        int newValue = JitStackFrameOperation.peekInt(0);
        int expectedValue = JitStackFrameOperation.peekInt(1);
        Offset off = JitStackFrameOperation.peekWord(2).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, ptr.compareAndSwapInt(off, expectedValue, newValue));
    }

    @BYTECODE_TEMPLATE(PCMPSWP_WORD)
    public static void pcmpswp_word() {
        Word newValue = JitStackFrameOperation.peekWord(0);
        Word expectedValue = JitStackFrameOperation.peekWord(1);
        Offset off = JitStackFrameOperation.peekWord(2).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeWord(0, ptr.compareAndSwapWord(off, expectedValue, newValue));
    }

    @BYTECODE_TEMPLATE(PCMPSWP_REFERENCE)
    public static void pcmpswp_reference() {
        Reference newValue = JitStackFrameOperation.peekReference(0);
        Reference expectedValue = JitStackFrameOperation.peekReference(1);
        Offset off = JitStackFrameOperation.peekWord(2).asOffset();
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeReference(0, ptr.compareAndSwapReference(off, expectedValue, newValue));
    }

    @BYTECODE_TEMPLATE(PCMPSWP_INT_I)
    public static void pcmpswp_int_i() {
        int newValue = JitStackFrameOperation.peekInt(0);
        int expectedValue = JitStackFrameOperation.peekInt(1);
        int off = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeInt(0, ptr.compareAndSwapInt(off, expectedValue, newValue));
    }

    @BYTECODE_TEMPLATE(PCMPSWP_WORD_I)
    public static void pcmpswp_word_i() {
        Word newValue = JitStackFrameOperation.peekWord(0);
        Word expectedValue = JitStackFrameOperation.peekWord(1);
        int off = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeWord(0, ptr.compareAndSwapWord(off, expectedValue, newValue));
    }

    @BYTECODE_TEMPLATE(PCMPSWP_REFERENCE_I)
    public static void pcmpswp_reference_i() {
        Reference newValue = JitStackFrameOperation.peekReference(0);
        Reference expectedValue = JitStackFrameOperation.peekReference(1);
        int off = JitStackFrameOperation.peekInt(2);
        Pointer ptr = JitStackFrameOperation.peekWord(3).asPointer();
        JitStackFrameOperation.removeSlots(3);
        JitStackFrameOperation.pokeReference(0, ptr.compareAndSwapReference(off, expectedValue, newValue));
    }

    @BYTECODE_TEMPLATE(MOV_I2F)
    public static void mov_i2f() {
        int value = JitStackFrameOperation.peekInt(0);
        JitStackFrameOperation.pokeFloat(0, SpecialBuiltin.intToFloat(value));
    }

    @BYTECODE_TEMPLATE(MOV_F2I)
    public static void mov_f2i() {
        float value = JitStackFrameOperation.peekFloat(0);
        JitStackFrameOperation.pokeInt(0, SpecialBuiltin.floatToInt(value));
    }

    @BYTECODE_TEMPLATE(MOV_L2D)
    public static void mov_l2d() {
        long value = JitStackFrameOperation.peekLong(0);
        JitStackFrameOperation.pokeDouble(0, SpecialBuiltin.longToDouble(value));
    }

    @BYTECODE_TEMPLATE(MOV_D2L)
    public static void mov_d2l() {
        double value = JitStackFrameOperation.peekDouble(0);
        JitStackFrameOperation.pokeLong(0, SpecialBuiltin.doubleToLong(value));
    }

    @BYTECODE_TEMPLATE(LSB)
    public static void lsb() {
        Word value2 = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.pokeInt(0, value2.leastSignificantBitSet());
    }

    @BYTECODE_TEMPLATE(MSB)
    public static void msb() {
        Word value2 = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.pokeInt(0, value2.mostSignificantBitSet());
    }

    @BYTECODE_TEMPLATE(MEMBAR_LOAD_LOAD)
    public static void membar_load_load() {
        loadLoad();
    }

    @BYTECODE_TEMPLATE(MEMBAR_LOAD_STORE)
    public static void membar_load_store() {
        loadStore();
    }

    @BYTECODE_TEMPLATE(MEMBAR_STORE_STORE)
    public static void membar_store_store() {
        loadStore();
    }

    @BYTECODE_TEMPLATE(MEMBAR_STORE_LOAD)
    public static void membar_store_load() {
        storeLoad();
    }

    @BYTECODE_TEMPLATE(PAUSE)
    public static void pause() {
        SpecialBuiltin.pause();
    }

    @BYTECODE_TEMPLATE(READREG$fp_cpu)
    public static void readreg_fp_cpu() {
        JitStackFrameOperation.pushWord(VMRegister.getCpuFramePointer());
    }

    @BYTECODE_TEMPLATE(READREG$sp_cpu)
    public static void readreg_sp_cpu() {
        JitStackFrameOperation.pushWord(VMRegister.getCpuStackPointer());
    }

    @BYTECODE_TEMPLATE(READREG$fp_abi)
    public static void readreg_fp_abi() {
        JitStackFrameOperation.pushWord(VMRegister.getAbiFramePointer());
    }

    @BYTECODE_TEMPLATE(READREG$sp_abi)
    public static void readreg_sp_abi() {
        JitStackFrameOperation.pushWord(VMRegister.getAbiStackPointer());
    }

    @BYTECODE_TEMPLATE(READREG$latch)
    public static void readreg_latch() {
        JitStackFrameOperation.pushWord(VMRegister.getSafepointLatchRegister());
    }

    @BYTECODE_TEMPLATE(WRITEREG$fp_cpu)
    public static void writereg_fp_cpu() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        VMRegister.setCpuFramePointer(value);
    }

    @BYTECODE_TEMPLATE(WRITEREG$sp_cpu)
    public static void writereg_sp_cpu() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        VMRegister.setCpuStackPointer(value);
    }

    @BYTECODE_TEMPLATE(WRITEREG$fp_abi)
    public static void writereg_fp_abi() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        VMRegister.setAbiFramePointer(value);
    }

    @BYTECODE_TEMPLATE(WRITEREG$sp_abi)
    public static void writereg_sp_abi() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        VMRegister.setAbiStackPointer(value);
    }

    @BYTECODE_TEMPLATE(WRITEREG$latch)
    public static void writereg_latch() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        VMRegister.setSafepointLatchRegister(value);
    }

    @PLATFORM(cpu = "sparc")
    @BYTECODE_TEMPLATE(WRITEREG$link)
    public static void writereg_link() {
        Word value = JitStackFrameOperation.peekWord(0);
        JitStackFrameOperation.removeSlots(1);
        VMRegister.setCallAddressRegister(value);
    }
}
