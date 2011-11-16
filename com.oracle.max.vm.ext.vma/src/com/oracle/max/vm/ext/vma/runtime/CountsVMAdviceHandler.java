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
package com.oracle.max.vm.ext.vma.runtime;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.thread.*;


public class CountsVMAdviceHandler extends VMAdviceHandler {

    static long[][] counts = new long[AdviceMethod.values().length][AdviceMode.values().length];

    @Override
    public void finalise() {
        System.out.println("Advice method counts");
        for (AdviceMethod am : AdviceMethod.values()) {
            System.out.printf("  %-20s B:%d, A:%d%n", am.name(), counts[am.ordinal()][0], counts[am.ordinal()][1]);
        }
    }

    @Override
    public void gcSurvivor(Pointer cell) {
    }

// START GENERATED CODE
// EDIT AND RUN CountVMAdviceHandlerGenerator.main() TO MODIFY

    enum AdviceMethod {
        GC,
        ThreadStarting,
        ThreadTerminating,
        New,
        NewArray,
        MultiNewArray,
        ConstLoad,
        Load,
        ArrayLoad,
        Store,
        ArrayStore,
        StackAdjust,
        Operation,
        Conversion,
        If,
        Bytecode,
        Return,
        GetStatic,
        PutStatic,
        GetField,
        PutField,
        InvokeVirtual,
        InvokeSpecial,
        InvokeStatic,
        InvokeInterface,
        ArrayLength,
        Throw,
        CheckCast,
        InstanceOf,
        MonitorEnter,
        MonitorExit,
        MethodEntry;
    }

    private static final int MAX_LENGTH = 17;

    @Override
    public void adviseBeforeGC() {
        counts[0][0]++;
    }

    @Override
    public void adviseAfterGC() {
        counts[0][1]++;
    }

    @Override
    public void adviseBeforeThreadStarting(VmThread arg1) {
        counts[1][0]++;
    }

    @Override
    public void adviseBeforeThreadTerminating(VmThread arg1) {
        counts[2][0]++;
    }

    @Override
    public void adviseAfterNew(Object arg1) {
        counts[3][1]++;
    }

    @Override
    public void adviseAfterNewArray(Object arg1, int arg2) {
        counts[4][1]++;
    }

    @Override
    public void adviseAfterMultiNewArray(Object arg1, int[] arg2) {
        counts[5][1]++;
    }

    @Override
    public void adviseBeforeConstLoad(double arg1) {
        counts[6][0]++;
    }

    @Override
    public void adviseBeforeConstLoad(Object arg1) {
        counts[6][0]++;
    }

    @Override
    public void adviseBeforeConstLoad(long arg1) {
        counts[6][0]++;
    }

    @Override
    public void adviseBeforeConstLoad(float arg1) {
        counts[6][0]++;
    }

    @Override
    public void adviseBeforeLoad(int arg1) {
        counts[7][0]++;
    }

    @Override
    public void adviseBeforeArrayLoad(Object arg1, int arg2) {
        counts[8][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, Object arg2) {
        counts[9][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, float arg2) {
        counts[9][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, double arg2) {
        counts[9][0]++;
    }

    @Override
    public void adviseBeforeStore(int arg1, long arg2) {
        counts[9][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, Object arg3) {
        counts[10][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, float arg3) {
        counts[10][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, long arg3) {
        counts[10][0]++;
    }

    @Override
    public void adviseBeforeArrayStore(Object arg1, int arg2, double arg3) {
        counts[10][0]++;
    }

    @Override
    public void adviseBeforeStackAdjust(int arg1) {
        counts[11][0]++;
    }

    @Override
    public void adviseBeforeOperation(int arg1, double arg2, double arg3) {
        counts[12][0]++;
    }

    @Override
    public void adviseBeforeOperation(int arg1, long arg2, long arg3) {
        counts[12][0]++;
    }

    @Override
    public void adviseBeforeOperation(int arg1, float arg2, float arg3) {
        counts[12][0]++;
    }

    @Override
    public void adviseBeforeConversion(int arg1, long arg2) {
        counts[13][0]++;
    }

    @Override
    public void adviseBeforeConversion(int arg1, float arg2) {
        counts[13][0]++;
    }

    @Override
    public void adviseBeforeConversion(int arg1, double arg2) {
        counts[13][0]++;
    }

    @Override
    public void adviseBeforeIf(int arg1, int arg2, int arg3) {
        counts[14][0]++;
    }

    @Override
    public void adviseBeforeIf(int arg1, Object arg2, Object arg3) {
        counts[14][0]++;
    }

    @Override
    public void adviseBeforeBytecode(int arg1) {
        counts[15][0]++;
    }

    @Override
    public void adviseBeforeReturn() {
        counts[16][0]++;
    }

    @Override
    public void adviseBeforeReturn(long arg1) {
        counts[16][0]++;
    }

    @Override
    public void adviseBeforeReturn(float arg1) {
        counts[16][0]++;
    }

    @Override
    public void adviseBeforeReturn(double arg1) {
        counts[16][0]++;
    }

    @Override
    public void adviseBeforeReturn(Object arg1) {
        counts[16][0]++;
    }

    @Override
    public void adviseBeforeGetStatic(Object arg1, int arg2) {
        counts[17][0]++;
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, float arg3) {
        counts[18][0]++;
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, long arg3) {
        counts[18][0]++;
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, double arg3) {
        counts[18][0]++;
    }

    @Override
    public void adviseBeforePutStatic(Object arg1, int arg2, Object arg3) {
        counts[18][0]++;
    }

    @Override
    public void adviseBeforeGetField(Object arg1, int arg2) {
        counts[19][0]++;
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, float arg3) {
        counts[20][0]++;
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, double arg3) {
        counts[20][0]++;
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, Object arg3) {
        counts[20][0]++;
    }

    @Override
    public void adviseBeforePutField(Object arg1, int arg2, long arg3) {
        counts[20][0]++;
    }

    @Override
    public void adviseBeforeInvokeVirtual(Object arg1, MethodActor arg2) {
        counts[21][0]++;
    }

    @Override
    public void adviseBeforeInvokeSpecial(Object arg1, MethodActor arg2) {
        counts[22][0]++;
    }

    @Override
    public void adviseBeforeInvokeStatic(Object arg1, MethodActor arg2) {
        counts[23][0]++;
    }

    @Override
    public void adviseBeforeInvokeInterface(Object arg1, MethodActor arg2) {
        counts[24][0]++;
    }

    @Override
    public void adviseBeforeArrayLength(Object arg1, int arg2) {
        counts[25][0]++;
    }

    @Override
    public void adviseBeforeThrow(Object arg1) {
        counts[26][0]++;
    }

    @Override
    public void adviseBeforeCheckCast(Object arg1, Object arg2) {
        counts[27][0]++;
    }

    @Override
    public void adviseBeforeInstanceOf(Object arg1, Object arg2) {
        counts[28][0]++;
    }

    @Override
    public void adviseBeforeMonitorEnter(Object arg1) {
        counts[29][0]++;
    }

    @Override
    public void adviseBeforeMonitorExit(Object arg1) {
        counts[30][0]++;
    }

    @Override
    public void adviseAfterInvokeVirtual(Object arg1, MethodActor arg2) {
        counts[21][1]++;
    }

    @Override
    public void adviseAfterInvokeSpecial(Object arg1, MethodActor arg2) {
        counts[22][1]++;
    }

    @Override
    public void adviseAfterInvokeStatic(Object arg1, MethodActor arg2) {
        counts[23][1]++;
    }

    @Override
    public void adviseAfterInvokeInterface(Object arg1, MethodActor arg2) {
        counts[24][1]++;
    }

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        counts[31][1]++;
    }

// END GENERATED CODE

}
