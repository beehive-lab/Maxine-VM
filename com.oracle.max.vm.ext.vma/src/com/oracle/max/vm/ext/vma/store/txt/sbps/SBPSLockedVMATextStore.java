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
package com.oracle.max.vm.ext.vma.store.txt.sbps;

import com.oracle.max.vm.ext.vma.store.txt.*;

/**
 * A synchronizing/locked version of {@link SBPSTextVMATextStore}.
 */
public class SBPSLockedVMATextStore extends SBPSVMATextStore {

    @Override
    protected synchronized void defineShortForm(CSFVMATextStore.ShortForm type, Object key, String shortForm, String classShortForm) {
        super.defineShortForm(type, key, shortForm, classShortForm);
    }

// START GENERATED CODE
// EDIT AND RUN SBPSLockedVMATextStoreGenerator.main() TO MODIFY

    @Override
    public synchronized void removal(long arg1) {
        super.removal(arg1);
    }

    @Override
    public synchronized void unseenObject(long arg1, String arg2, long arg3, String arg4, long arg5) {
        super.unseenObject(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeGC(long arg1, String arg2) {
        super.adviseBeforeGC(arg1, arg2);
    }

    @Override
    public synchronized void adviseAfterGC(long arg1, String arg2) {
        super.adviseAfterGC(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeThreadStarting(long arg1, String arg2) {
        super.adviseBeforeThreadStarting(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeThreadTerminating(long arg1, String arg2) {
        super.adviseBeforeThreadTerminating(arg1, arg2);
    }

    @Override
    public synchronized void adviseBeforeReturnByThrow(long arg1, String arg2, int arg3, long arg4, int arg5) {
        super.adviseBeforeReturnByThrow(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeConstLoad(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(long arg1, String arg2, int arg3, float arg4) {
        super.adviseBeforeConstLoad(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeConstLoad(long arg1, String arg2, int arg3, double arg4) {
        super.adviseBeforeConstLoad(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeConstLoadObject(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeConstLoadObject(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeLoad(long arg1, String arg2, int arg3, int arg4) {
        super.adviseBeforeLoad(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeArrayLoad(long arg1, String arg2, int arg3, long arg4, int arg5) {
        super.adviseBeforeArrayLoad(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseBeforeStore(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, float arg5) {
        super.adviseBeforeStore(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, double arg5) {
        super.adviseBeforeStore(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeStoreObject(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseBeforeStoreObject(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, float arg6) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, double arg6) {
        super.adviseBeforeArrayStore(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeArrayStoreObject(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        super.adviseBeforeArrayStoreObject(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeStackAdjust(long arg1, String arg2, int arg3, int arg4) {
        super.adviseBeforeStackAdjust(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, long arg5, long arg6) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, float arg5, float arg6) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, double arg5, double arg6) {
        super.adviseBeforeOperation(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, float arg5) {
        super.adviseBeforeConversion(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseBeforeConversion(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, double arg5) {
        super.adviseBeforeConversion(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeIf(long arg1, String arg2, int arg3, int arg4, int arg5, int arg6, int arg7) {
        super.adviseBeforeIf(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeIfObject(long arg1, String arg2, int arg3, int arg4, long arg5, long arg6, int arg7) {
        super.adviseBeforeIfObject(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeGoto(long arg1, String arg2, int arg3, int arg4) {
        super.adviseBeforeGoto(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeReturnObject(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeReturnObject(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeReturn(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeReturn(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeReturn(long arg1, String arg2, int arg3, float arg4) {
        super.adviseBeforeReturn(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeReturn(long arg1, String arg2, int arg3, double arg4) {
        super.adviseBeforeReturn(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeReturn(long arg1, String arg2, int arg3) {
        super.adviseBeforeReturn(arg1, arg2, arg3);
    }

    @Override
    public synchronized void adviseBeforeGetStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6) {
        super.adviseBeforeGetStatic(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforePutStaticObject(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, long arg7) {
        super.adviseBeforePutStaticObject(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, double arg7) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, long arg7) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, float arg7) {
        super.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeGetField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeGetField(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforePutFieldObject(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, long arg8) {
        super.adviseBeforePutFieldObject(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Override
    public synchronized void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, double arg8) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Override
    public synchronized void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, long arg8) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Override
    public synchronized void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, float arg8) {
        super.adviseBeforePutField(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Override
    public synchronized void adviseBeforeInvokeVirtual(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeVirtual(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeInvokeSpecial(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeSpecial(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeInvokeStatic(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeStatic(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeInvokeInterface(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeInterface(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseBeforeArrayLength(long arg1, String arg2, int arg3, long arg4, int arg5) {
        super.adviseBeforeArrayLength(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseBeforeThrow(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeThrow(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeCheckCast(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        super.adviseBeforeCheckCast(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeInstanceOf(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        super.adviseBeforeInstanceOf(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseBeforeMonitorEnter(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeMonitorEnter(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseBeforeMonitorExit(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeMonitorExit(arg1, arg2, arg3, arg4);
    }

    @Override
    public synchronized void adviseAfterLoadObject(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseAfterLoadObject(arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public synchronized void adviseAfterArrayLoadObject(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        super.adviseAfterArrayLoadObject(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseAfterNew(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        super.adviseAfterNew(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public synchronized void adviseAfterNewArray(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, int arg7) {
        super.adviseAfterNewArray(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseAfterMultiNewArray(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, int arg7) {
        super.adviseAfterMultiNewArray(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public synchronized void adviseAfterMethodEntry(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseAfterMethodEntry(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

// END GENERATED CODE
}
