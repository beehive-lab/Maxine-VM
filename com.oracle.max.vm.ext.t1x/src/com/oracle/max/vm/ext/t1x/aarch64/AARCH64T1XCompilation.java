/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR
 * THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for
 * more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need
 * additional information or have any questions.
 */
package com.oracle.max.vm.ext.t1x.aarch64;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import java.io.*;
import java.util.concurrent.atomic.*;

import com.oracle.max.asm.target.aarch64.Aarch64Assembler.ConditionFlag;
import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.aarch64.*;
import com.sun.max.vm.type.*;

// import java.io.PrintWriter;

public class AARCH64T1XCompilation extends T1XCompilation {

    public static AtomicInteger methodCounter = new AtomicInteger(536870912);
    private static final Object fileLock = new Object();
    private static File file;

    private static boolean DEBUG_METHODS = true;

    protected final Aarch64MacroAssembler asm;
    final PatchInfoAARCH64 patchInfo;
    public static boolean FLOATDOUBLEREGISTERS = true;

    public AARCH64T1XCompilation(T1X compiler) {
        super(compiler);
        asm = new Aarch64MacroAssembler(target(), null);
        buf = asm.codeBuffer;
        patchInfo = new PatchInfoAARCH64();
    }

    public void setDebug(boolean value) {
        DEBUG_METHODS = value;
    }

    static {
        initDebugMethods();
    }

    public static void initDebugMethods() {
        if ((file = new File(getDebugMethodsPath() + "debugT1Xmethods")).exists()) {
            file.delete();
        }
        file = new File(getDebugMethodsPath() + "debugT1Xmethods");
    }

    public static void writeDebugMethod(String name, int index) throws Exception {
        synchronized (fileLock) {
            try {
                assert DEBUG_METHODS;
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(index + " " + name + "\n");
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String getDebugMethodsPath() {
        return System.getenv("MAXINE_HOME") + "/maxine-tester/junit-tests/";

    }

    @Override
    protected void initFrame(ClassMethodActor method, CodeAttribute codeAttribute) {
        int maxLocals = codeAttribute.maxLocals;
        int maxStack = codeAttribute.maxStack;
        int maxParams = method.numberOfParameterSlots();
        if (method.isSynchronized() && !method.isStatic()) {
            synchronizedReceiver = maxLocals++;
        }
        frame = new AARCH64JVMSFrameLayout(maxLocals, maxStack, maxParams, T1XTargetMethod.templateSlots());
    }

    public Aarch64MacroAssembler getMacroAssembler() {
        return asm;
    }

    @Override
    public void decStack(int numberOfSlots) {
        assert numberOfSlots > 0;
        asm.add(64, sp, sp, numberOfSlots * JVMS_SLOT_SIZE);
    }

    @Override
    public void incStack(int numberOfSlots) {
        assert numberOfSlots > 0;
        asm.sub(64, sp, sp, numberOfSlots * JVMS_SLOT_SIZE);
    }

    @Override
    protected void adjustReg(CiRegister reg, int delta) {
    	asm.incrementl(reg, delta);
    }

    @Override
    public void peekObject(CiRegister dst, int index) {
    	CiAddress a = spWord(index);
    	asm.ldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void pokeObject(CiRegister src, int index) {
    	CiAddress a = spWord(index);
    	asm.str(64, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void peekWord(CiRegister dst, int index) {
    	CiAddress address = spWord(index);
    	asm.ldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
    }

    @Override
    public void pokeWord(CiRegister src, int index) {
    	CiAddress address = spWord(index);
    	asm.str(64, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
    }

    @Override
    public void peekInt(CiRegister dst, int index) {
    	CiAddress a = spInt(index);
    	asm.ldr(32, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void pokeInt(CiRegister src, int index) {
    	CiAddress a = spInt(index);
    	asm.str(32, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void peekLong(CiRegister dst, int index) {
    	CiAddress a = spLong(index);
    	asm.ldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void pokeLong(CiRegister src, int index) {
    	CiAddress a = spLong(index);
    	asm.str(64, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void peekDouble(CiRegister dst, int index) {
        assert dst.isFpu();
        CiAddress a = spLong(index);
        asm.fldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void pokeDouble(CiRegister src, int index) {
    	assert src.isFpu();
    	CiAddress a = spLong(index);
    	asm.fstr(64, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void peekFloat(CiRegister dst, int index) {
        assert dst.isFpu();
        CiAddress a = spInt(index);
        asm.fldr(32, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void pokeFloat(CiRegister src, int index) {
    	assert src.isFpu();
    	CiAddress a = spInt(index);
    	asm.fstr(32, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void assignObjectReg(CiRegister dst, CiRegister src) {

    }

    @Override
    protected void assignWordReg(CiRegister dst, CiRegister src) {

    }

    @Override
    protected void assignLong(CiRegister dst, long value) {
    	asm.mov64BitConstant(dst, value);
    }

    @Override
    protected void assignObject(CiRegister dst, Object value) {

    }

    @Override
    protected void loadInt(CiRegister dst, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.INT));
    	asm.ldr(32, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void loadLong(CiRegister dst, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.LONG));
    	asm.ldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void loadWord(CiRegister dst, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
    	asm.ldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void loadObject(CiRegister dst, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
    	asm.ldr(64, dst, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void storeInt(CiRegister src, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.INT));
    	asm.str(32, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void storeLong(CiRegister src, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.LONG));
    	asm.str(64, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void storeWord(CiRegister src, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
    	asm.str(64, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    protected void storeObject(CiRegister src, int index) {
    	CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
    	asm.str(64, src, Aarch64Address.createUnscaledImmediateAddress(a.base(), a.displacement));
    }

    @Override
    public void assignInt(CiRegister dst, int value) {
    	asm.mov32BitConstant(dst, value);
    }

    @Override
    protected void assignFloat(CiRegister dst, float value) {
    	asm.mov32BitConstant(scratch, Float.floatToRawIntBits(value));
    	asm.fmovCpu2Fpu(32, dst, scratch);
    }

    @Override
    protected void do_store(int index, Kind kind) {

    }

    @Override
    protected void do_load(int index, Kind kind) {

    }

    @Override
    protected void do_oconst(Object value) {

    }

    @Override
    protected void do_iconst(int value) {

    }

    @Override
    protected void do_iinc(int index, int increment) {

    }

    @Override
    protected void do_fconst(float value) {

    }

    @Override
    protected void do_dconst(double value) {

    }

    @Override
    protected void do_lconst(long value) {

    }

    @Override
    protected void assignDouble(CiRegister dst, double value) {
    	asm.mov64BitConstant(scratch, Double.doubleToRawLongBits(value));
    	asm.fmovCpu2Fpu(64, dst, scratch);
    }

    @Override
    protected int callDirect() {
        return 0;
    }

    @Override
    protected int callDirect(int receiverStackIndex) {
        return 0;
    }

    @Override
    protected int callIndirect(CiRegister target, int receiverStackIndex) {
        return 0;
    }

    @Override
    protected void nullCheck(CiRegister src) {

    }

    private void alignDirectCall(int callPos) {

    }

    private int framePointerAdjustment() {
        return 0;
    }

    @Override
    protected Adapter emitPrologue() {
        return null;
    }

    @Override
    protected void emitUnprotectMethod() {

    }

    @Override
    protected void emitEpilogue() {

    }

    @Override
    protected void do_preVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {

    }

    @Override
    protected void do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {

    }

    @Override
    protected void do_tableswitch() {

    }

    @Override
    protected void do_lookupswitch() {

    }

    @Override
    public void cleanup() {
        patchInfo.size = 0;
        super.cleanup();
    }

    @Override
    protected void branch(int opcode, int targetBCI, int bci) {
    }

    @Override
    protected void addObjectLiteralPatch(int index, int patchPos) {
        final int dispPos = patchPos;
        patchInfo.addObjectLiteral(dispPos, index);
    }

    @Override
    protected void fixup() {

    }

    public static int movqDisp(int dispPos, int dispFromCodeStart) {
        return 0;
    }

    @HOSTED_ONLY
    public static int[] findDataPatchPosns(MaxTargetMethod source, int dispFromCodeStart) {

        return null;
    }

    static class PatchInfoAARCH64 extends PatchInfo {

        /**
         * Denotes a conditional jump patch. Encoding: {@code cc, pos, targetBCI}.
         */
        static final int JCC = 0;

        /**
         * Denotes an unconditional jump patch. Encoding: {@code pos, targetBCI}.
         */
        static final int JMP = 1;

        /**
         * Denotes a signed int jump table entry. Encoding: {@code pos, jumpTablePos, targetBCI}.
         */
        static final int JUMP_TABLE_ENTRY = 2;

        /**
         * Denotes a signed int jump table entry. Encoding: {@code pos, key, lookupTablePos, targetBCI}.
         */
        static final int LOOKUP_TABLE_ENTRY = 3;

        /**
         * Denotes a movq instruction that loads an object literal. Encoding: {@code dispPos, index}.
         */
        static final int OBJECT_LITERAL = 4;

        void addJCC(ConditionFlag cc, int pos, int targetBCI) {
            ensureCapacity(size + 4);
            data[size++] = JCC;
            data[size++] = cc.ordinal();
            data[size++] = pos;
            data[size++] = targetBCI;
        }

        void addJMP(int pos, int targetBCI) {
            ensureCapacity(size + 3);
            data[size++] = JMP;
            data[size++] = pos;
            data[size++] = targetBCI;
        }

        void addJumpTableEntry(int pos, int jumpTablePos, int targetBCI) {
            ensureCapacity(size + 4);
            data[size++] = JUMP_TABLE_ENTRY;
            data[size++] = pos;
            data[size++] = jumpTablePos;
            data[size++] = targetBCI;
        }

        void addLookupTableEntry(int pos, int key, int lookupTablePos, int targetBCI) {
            ensureCapacity(size + 5);
            data[size++] = LOOKUP_TABLE_ENTRY;
            data[size++] = pos;
            data[size++] = key;
            data[size++] = lookupTablePos;
            data[size++] = targetBCI;
        }

        void addObjectLiteral(int dispPos, int index) {
            ensureCapacity(size + 3);
            data[size++] = OBJECT_LITERAL;
            data[size++] = dispPos;
            data[size++] = index;
        }

    }

    @Override
    protected Kind invokeKind(SignatureDescriptor signature) {
        Kind returnKind = signature.resultKind();
        if (returnKind.stackKind == Kind.INT) {
            return Kind.WORD;
        }
        return returnKind;
    }

    @Override
    protected void do_dup() {

    }

    @Override
    protected void do_dup_x1() {

    }

    @Override
    protected void do_dup_x2() {

    }

    @Override
    protected void do_dup2() {

    }

    @Override
    protected void do_dup2_x1() {

    }

    @Override
    protected void do_dup2_x2() {

    }

    @Override
    protected void do_swap() {

    }

    @Override
    protected void emit(T1XTemplateTag tag) {
        start(tag);
        finish();
    }

    public void emitPrologueTests() {
        /*
         * mimics the functionality of T1XCompilation::compile2
         */
        emitPrologue();

        emitUnprotectMethod();

        // do_profileMethodEntry();

        do_methodTraceEntry();

        // do_synchronizedMethodAcquire();

        // int bci = 0;
        // int endBCI = stream.endBCI();
        // while (bci < endBCI) {
        // int opcode = stream.currentBC();
        // processBytecode(opcode);
        // stream.next();
        // bci = stream.currentBCI();
        // }

        // int epiloguePos = buf.position();

        // do_synchronizedMethodHandler(method, endBCI);

        // if (epiloguePos != buf.position()) {
        // bciToPos[endBCI] = epiloguePos;
        // }
    }

    public void do_iaddTests() {

    }

    public void do_imulTests() {

    }

    public void do_initFrameTests(ClassMethodActor method, CodeAttribute codeAttribute) {
        initFrame(method, codeAttribute);
    }

    public void do_storeTests(int index, Kind kind) {
        do_store(index, kind);
    }

    public void do_loadTests(int index, Kind kind) {
        do_load(index, kind);
    }

    public void do_fconstTests(float value) {
        do_fconst(value);
    }

    public void do_dconstTests(double value) {
        do_dconst(value);
    }

    public void do_iconstTests(int value) {
        do_iconst(value);
    }

    public void do_lconstTests(long value) {
        do_lconst(value);
    }

    public void assignmentTests(CiRegister reg, long value) {
        assignLong(reg, value);
    }

    public void assignDoubleTest(CiRegister reg, double value) {
        assignDouble(reg, value);
    }
}
