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

import com.oracle.max.vm.ext.vma.log.*;

/**
 * Debugging aid, runs advice handlers but dumps output.
 */
public class NullVMAdviceHandlerLog extends VMAdviceHandlerLog {

    @Override
    public boolean initializeLog(boolean timeOrdered) {

        return false;
    }

    @Override
    public void finalizeLog() {


    }

    @Override
    public void removal(long id) {


    }

    @Override
    public void unseenObject(String threadName, long objId, String className, long clId) {


    }

    @Override
    public void resetTime() {


    }

    @Override
    public void adviseBeforeGC(String threadName) {


    }

    @Override
    public void adviseAfterGC(String threadName) {


    }

    @Override
    public void adviseBeforeBytecode(String threadName, int code) {

    }

    @Override
    public void adviseBeforeThreadStarting(String threadName) {


    }

    @Override
    public void adviseBeforeThreadTerminating(String threadName) {


    }

    @Override
    public void adviseBeforeConstLoad(String threadName, long value) {


    }

    @Override
    public void adviseBeforeConstLoadObject(String threadName, long value) {


    }

    @Override
    public void adviseBeforeConstLoad(String threadName, float value) {


    }

    @Override
    public void adviseBeforeConstLoad(String threadName, double value) {


    }

    @Override
    public void adviseBeforeLoad(String threadName, int arg1) {


    }

    @Override
    public void adviseBeforeArrayLoad(String threadName, long objId, int index) {


    }

    @Override
    public void adviseBeforeStore(String threadName, int dispToLocalSlot, long value) {


    }

    @Override
    public void adviseBeforeStore(String threadName, int dispToLocalSlot, float value) {


    }

    @Override
    public void adviseBeforeStore(String threadName, int dispToLocalSlot, double value) {


    }

    @Override
    public void adviseBeforeStoreObject(String threadName, int dispToLocalSlot, long value) {


    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, float value) {


    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, long value) {


    }

    @Override
    public void adviseBeforeArrayStore(String threadName, long objId, int index, double value) {


    }

    @Override
    public void adviseBeforeArrayStoreObject(String threadName, long objId, int index, long value) {


    }

    @Override
    public void adviseBeforeStackAdjust(String threadName, int arg1) {


    }

    @Override
    public void adviseBeforeOperation(String threadName, int arg1, long arg2, long arg3) {


    }

    @Override
    public void adviseBeforeOperation(String threadName, int arg1, float arg2, float arg3) {


    }

    @Override
    public void adviseBeforeOperation(String threadName, int arg1, double arg2, double arg3) {


    }

    @Override
    public void adviseBeforeConversion(String threadName, int arg1, long arg2) {


    }

    @Override
    public void adviseBeforeConversion(String threadName, int arg1, float arg2) {


    }

    @Override
    public void adviseBeforeConversion(String threadName, int arg1, double arg2) {


    }

    @Override
    public void adviseBeforeIf(String threadName, int opcode, int op1, int op2) {


    }

    @Override
    public void adviseBeforeIfObject(String threadName, int opcode, long objId1, long objId2) {


    }

    @Override
    public void adviseBeforeReturnObject(String threadName, long value) {


    }

    @Override
    public void adviseBeforeReturn(String threadName, long value) {


    }

    @Override
    public void adviseBeforeReturn(String threadName, float value) {


    }

    @Override
    public void adviseBeforeReturn(String threadName, double value) {


    }

    @Override
    public void adviseBeforeReturn(String threadName) {


    }

    @Override
    public void adviseBeforeGetStatic(String threadName, String className, long clId, String fieldName) {


    }

    @Override
    public void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, double value) {


    }

    @Override
    public void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, long value) {


    }

    @Override
    public void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, float value) {


    }

    @Override
    public void adviseBeforePutStaticObject(String threadName, String className, long clId, String fieldName, long value) {


    }

    @Override
    public void adviseBeforeGetField(String threadName, long objId, String className, long clId, String fieldName) {


    }

    @Override
    public void adviseBeforePutField(String threadName, long objId, String className, long clId, String fieldName, double value) {


    }

    @Override
    public void adviseBeforePutField(String threadName, long objId, String className, long clId, String fieldName, long value) {


    }

    @Override
    public void adviseBeforePutField(String threadName, long objId, String className, long clId, String fieldName, float value) {


    }

    @Override
    public void adviseBeforePutFieldObject(String threadName, long objId, String className, long clId, String fieldName, long value) {


    }

    @Override
    public void adviseBeforeInvokeVirtual(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseBeforeInvokeSpecial(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseBeforeInvokeStatic(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseBeforeInvokeInterface(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseBeforeArrayLength(String threadName, long objId, int length) {


    }

    @Override
    public void adviseBeforeThrow(String threadName, long objId) {


    }

    @Override
    public void adviseBeforeCheckCast(String threadName, long objId, String className, long clId) {


    }

    @Override
    public void adviseBeforeInstanceOf(String threadName, long objId, String className, long clId) {


    }

    @Override
    public void adviseBeforeMonitorEnter(String threadName, long objId) {


    }

    @Override
    public void adviseBeforeMonitorExit(String threadName, long objId) {


    }

    @Override
    public void adviseAfterInvokeVirtual(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseAfterInvokeSpecial(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseAfterMethodEntry(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseAfterInvokeStatic(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseAfterInvokeInterface(String threadName, long objId, String className, long clId, String methodName) {


    }

    @Override
    public void adviseAfterNew(String threadName, long objId, String className, long clId) {


    }

    @Override
    public void adviseAfterNewArray(String threadName, long objId, String className, long clId, int length) {


    }

    @Override
    public void adviseAfterMultiNewArray(String threadName, long objId, String className, long clId, int length) {


    }

}
