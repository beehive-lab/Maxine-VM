/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers.log.txt.sbps;

/**
 * A synchronizing/locked version of {@link SBPSTextVMAdviceHandlerLog}.
 */
public class SBPSLockedTextVMHandlerLog extends SBPSTextVMAdviceHandlerLog {

// START GENERATED CODE
// EDIT AND RUN SBPSLockedVMAdviceHandlerLogGenerator.main() TO MODIFY

    @Override
    public synchronized void adviseBeforeGC(String arg1) {
        super.adviseBeforeGC(arg1);
    }

    @Override
    public synchronized void adviseAfterGC(String arg1) {
        super.adviseAfterGC(arg1);
    }

    @Override
    public synchronized void adviseBeforeThreadStarting(String arg1) {
        super.adviseBeforeThreadStarting(arg1);
    }

    @Override
    public synchronized void adviseBeforeThreadTerminating(String arg1) {
        super.adviseBeforeThreadTerminating(arg1);
    }

    @Override
    public synchronized void adviseBeforeReturnByThrow(String arg1, long arg2, int arg3) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String arg1, float arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String arg1, double arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String arg1, long arg2) {
        super.adviseBeforeConstLoad(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeConstLoadObject(String arg1, long arg2) {
        super.adviseBeforeConstLoadObject(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeLoad(String arg1, int arg2) {
        super.adviseBeforeLoad(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeArrayLoad(String arg1, long arg2, int arg3) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(String arg1, int arg2, long arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(String arg1, int arg2, float arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStore(String arg1, int arg2, double arg3) {
        super.adviseBeforeStore(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeStoreObject(String arg1, int arg2, long arg3) {
        super.adviseBeforeStoreObject(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String arg1, long arg2, int arg3, double arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String arg1, long arg2, int arg3, float arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String arg1, long arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayStoreObject(String arg1, long arg2, int arg3, long arg4) {
        super.adviseBeforeArrayStoreObject(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeStackAdjust(String arg1, int arg2) {
        super.adviseBeforeStackAdjust(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeOperation(String arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeOperation(String arg1, int arg2, float arg3, float arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeOperation(String arg1, int arg2, double arg3, double arg4) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeConversion(String arg1, int arg2, double arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeConversion(String arg1, int arg2, float arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeConversion(String arg1, int arg2, long arg3) {
        super.adviseBeforeConversion(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeIf(String arg1, int arg2, int arg3, int arg4) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeIfObject(String arg1, int arg2, long arg3, long arg4) {
        super.adviseBeforeIfObject(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeBytecode(String arg1, int arg2) {
        super.adviseBeforeBytecode(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(String arg1) {
        super.adviseBeforeReturn(arg1);
    }

    @Override
    public synchronized void adviseBeforeReturn(String arg1, long arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(String arg1, float arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturn(String arg1, double arg2) {
        super.adviseBeforeReturn(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturnObject(String arg1, long arg2) {
        super.adviseBeforeReturnObject(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeGetStatic(String arg1, String arg2, long arg3, String arg4) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforePutStaticObject(String arg1, String arg2, long arg3, String arg4, long arg5) {
        super.adviseBeforePutStaticObject(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, long arg5) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, double arg5) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, float arg5) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeGetField(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseBeforeGetField(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforePutFieldObject(String arg1, long arg2, String arg3, long arg4, String arg5, long arg6) {
        super.adviseBeforePutFieldObject(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4, String arg5, float arg6) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4, String arg5, double arg6) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4, String arg5, long arg6) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeInvokeVirtual(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeInvokeSpecial(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeInvokeStatic(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeInvokeInterface(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeArrayLength(String arg1, long arg2, int arg3) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeThrow(String arg1, long arg2) {
        super.adviseBeforeThrow(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeCheckCast(String arg1, long arg2, String arg3, long arg4) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeInstanceOf(String arg1, long arg2, String arg3, long arg4) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeMonitorEnter(String arg1, long arg2) {
        super.adviseBeforeMonitorEnter(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeMonitorExit(String arg1, long arg2) {
        super.adviseBeforeMonitorExit(arg1, arg2);
    }

    @Override
    public synchronized void adviseAfterInvokeVirtual(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseAfterInvokeVirtual(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterInvokeSpecial(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseAfterInvokeSpecial(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterInvokeStatic(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseAfterInvokeStatic(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterInvokeInterface(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseAfterInvokeInterface(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterNew(String arg1, long arg2, String arg3, long arg4) {
        super.adviseAfterNew(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseAfterNewArray(String arg1, long arg2, String arg3, long arg4, int arg5) {
        super.adviseAfterNewArray(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterMultiNewArray(String arg1, long arg2, String arg3, long arg4, int arg5) {
        super.adviseAfterMultiNewArray(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterMethodEntry(String arg1, long arg2, String arg3, long arg4, String arg5) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void removal(long arg1) {
        super.removal(arg1);
    }

    @Override
    public synchronized void finalizeLog() {
        super.finalizeLog();
    }

    @Override
    public synchronized void unseenObject(String arg1, long arg2, String arg3, long arg4) {
        super.unseenObject(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void resetTime() {
        super.resetTime();
    }

// END GENERATED CODE
}
