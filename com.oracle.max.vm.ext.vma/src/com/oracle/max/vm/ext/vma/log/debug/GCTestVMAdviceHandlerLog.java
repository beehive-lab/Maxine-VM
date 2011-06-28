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
package com.oracle.max.vm.ext.vma.log.debug;

import java.util.*;

import com.oracle.max.vm.ext.vma.log.*;
import com.sun.max.vm.*;


public class GCTestVMAdviceHandlerLog extends VMAdviceHandlerLog {

    private Random random = new Random(46737);
    private boolean gcAlways;


    private void randomlyGC() {
        int next = random.nextInt(100);
        if (next == 57 || gcAlways) {
            Log.println("GCTestVMAdviceHandlerLog.GC");
            System.gc();
        }
    }

    @Override
    public boolean initializeLog() {
        gcAlways = System.getProperty("max.vma.gctest.everytime") != null;
        return true;
    }

    @Override
    public void finalizeLog() {
    }

    @Override
    public void removal(long id) {
        randomlyGC();

    }

    @Override
    public void unseenObject(String threadName, long objId, String className, long clId) {
        randomlyGC();

    }

    @Override
    public void resetTime() {
        randomlyGC();

    }

    @Override
    public void adviseBeforeGC(String threadName) {
        randomlyGC();

    }

    @Override
    public void adviseAfterGC(String threadName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeThreadStarting(String threadName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeThreadTerminating(String threadName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConstLoad(String threadName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConstLoadObject(String threadName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConstLoad(String threadName, float value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConstLoad(String threadName, double value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeIPush(String threadName, int arg1) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeLoad(String threadName, int arg1) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeArrayLoad(String threadName, long objId, int index) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeStore(String threadName, int dispToLocalSlot, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeStore(String threadName, int dispToLocalSlot, float value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeStore(String threadName, int dispToLocalSlot, double value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeStoreObject(String threadName, int dispToLocalSlot, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, float value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, double value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeArrayStoreObject(String threadName, long objId, int index, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeStackAdjust(String threadName, int arg1) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeOperation(String threadName, int arg1, long arg2, long arg3) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeOperation(String threadName, int arg1, float arg2, float arg3) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeOperation(String threadName, int arg1, double arg2, double arg3) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeIInc(String threadName, int arg1, int arg2, int arg3) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConversion(String threadName, int arg1, long arg2) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConversion(String threadName, int arg1, float arg2) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeConversion(String threadName, int arg1, double arg2) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeIf(String threadName, int opcode, int op1, int op2) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeIfObject(String threadName, int opcode, long objId1, long objId2) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeReturnObject(String threadName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeReturn(String threadName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeReturn(String threadName, float value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeReturn(String threadName, double value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeReturn(String threadName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeGetStatic(String threadName, String className, long clId, String fieldName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, double value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, float value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutStaticObject(String threadName, String className, long clId, String fieldName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeGetField(String threadName, long objId, String className, long clId, String fieldName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutField(String threadName, long objId, String className, long clId, String fieldName, double value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutField(String threadName, long objId, String className, long clId, String fieldName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutField(String threadName, long objId, String className, long clId, String fieldName, float value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforePutFieldObject(String threadName, long objId, String className, long clId, String fieldName, long value) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeInvokeVirtual(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeInvokeSpecial(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeInvokeStatic(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeInvokeInterface(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeArrayLength(String threadName, long objId, int length) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeThrow(String threadName, long objId) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeCheckCast(String threadName, long objId, String className, long clId) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeInstanceOf(String threadName, long objId, String className, long clId) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeMonitorEnter(String threadName, long objId) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeMonitorExit(String threadName, long objId) {
        randomlyGC();

    }

    @Override
    public void adviseBeforeBytecode(String threadName, int arg1) {
        randomlyGC();

    }

    @Override
    public void adviseAfterInvokeVirtual(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseAfterInvokeSpecial(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseAfterInvokeStatic(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseAfterInvokeInterface(String threadName, long objId, String className, long clId, String methodName) {
        randomlyGC();

    }

    @Override
    public void adviseAfterNew(String threadName, long objId, String className, long clId) {
        randomlyGC();

    }

    @Override
    public void adviseAfterNewArray(String threadName, long objId, String className, long clId, int length) {
        randomlyGC();

    }

    @Override
    public void adviseAfterMultiNewArray(String threadName, long objId, String className, long clId, int length) {
        randomlyGC();

    }

}
