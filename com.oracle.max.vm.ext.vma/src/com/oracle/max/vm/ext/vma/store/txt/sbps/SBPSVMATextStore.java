/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.max.vm.ext.vma.store.txt.ShortFormHandler.*;
import com.sun.max.program.*;

/**
 * This is the default implementation of {@link SBPSRawVMATextStore}. It translates
 * thread, class, field and method names into shorts forms using {@link ShortFormHandler}.
 *
 * N.B. The classloader id associated with a class, cf. {@link ClassNameId} is only output when the
 * short form of the class is defined.
 */
public class SBPSVMATextStore extends SBPSRawVMATextStore {

    private ThisShortFormHandler shortFormHandler;

    public SBPSVMATextStore() {

    }

    protected SBPSVMATextStore(String threadName) {
        super(threadName);
    }

    @Override
    public boolean initializeStore(boolean threadBatched, boolean perThread, PerThreadStoreOwner storeOwner) {
        boolean result = super.initializeStore(threadBatched, perThread, storeOwner);
        shortFormHandler = new ThisShortFormHandler(this);
        return result;
    }

    @Override
    public VMATextStore newThread(String threadName) {
        // The control flow is a little awkward and requires synchronization
        // due to having to save the created store in the short form handler.
        synchronized (shortFormHandler) {
            // This indirectly causes causes a call to defineThread where the
            // store is actually created.
            getThreadShortForm(threadName);
            return shortFormHandler.threadStore;
        }
    }

    private SBPSVMATextStore defineThread(String shortThreadName) {
        return (SBPSVMATextStore) super.newThread(shortThreadName);
    }

    @Override
    protected SBPSVMATextStore createThreadStore(String threadName) {
        return new SBPSVMATextStore(threadName);
    }

    @Override
    public void unseenObject(long time, String threadName, long objId, String className, long clId) {
        super.unseenObject(time, getThreadShortForm(threadName), checkRepeatId(objId, threadName), getClassShortForm(className, clId), clId);
    }

    private String getThreadShortForm(String threadName) {
        return shortFormHandler.getThreadShortForm(threadName);
    }

    private String getClassShortForm(String className, long clId) {
        return shortFormHandler.getClassShortForm(className, clId);
    }

    private String getFieldShortForm(String className, long clId, String fieldName) {
        return shortFormHandler.getFieldShortForm(className, clId, fieldName);
    }

    private String getMethodShortForm(String className, long clId, String fieldName) {
        return shortFormHandler.getMethodShortForm(className, clId, fieldName);
    }

    private static class ThisShortFormHandler extends ShortFormHandler {
        SBPSVMATextStore globalStore;
        SBPSVMATextStore threadStore;

        ThisShortFormHandler(SBPSVMATextStore store) {
            this.globalStore = store;
        }



        @Override
        protected void defineShortForm(ShortFormHandler.ShortForm type, Object key, String shortForm, String classShortForm) {
            ClassNameId className = null;
            SBPSVMATextStore store = globalStore;

            if (type == ShortForm.T) {
                // This is where we first find out about a new thread, when creating the short form in newThread
                // If we are in per-thread mode, we continue with the returned thread-specific store.
                threadStore = globalStore.defineThread(shortForm);
                store = threadStore;
            }
            if (type == ShortForm.C) {
                className = (ClassNameId) key;
                store.appendClassShortForm(className.name, className.clId, shortForm);
            } else if (type == ShortForm.T) {
                store.appendThreadShortForm((String) key, shortForm);
            } else {
                // F/M
                QualName qualName = (QualName) key;
                // guaranteed to have already created the short form for the class name
                store.appendMemberShortForm(type == ShortForm.F ? VMATextStoreFormat.Key.FIELD_DEFINITION : VMATextStoreFormat.Key.METHOD_DEFINITION,
                                classShortForm, qualName.name, shortForm);
            }
        }
    }

// START GENERATED CODE
// EDIT AND RUN SBPSVMATextStoreGenerator.main() TO MODIFY

    @Override
    public void adviseAfterMultiNewArray(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, int arg7) {
        ProgramError.unexpected("adviseAfterMultiNewArray");
    }

    @Override
    public void adviseBeforeGC(long arg1, String arg2) {
        super.adviseBeforeGC(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseAfterGC(long arg1, String arg2) {
        super.adviseAfterGC(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseBeforeThreadStarting(long arg1, String arg2) {
        super.adviseBeforeThreadStarting(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseBeforeThreadTerminating(long arg1, String arg2) {
        super.adviseBeforeThreadTerminating(arg1, getThreadShortForm(arg2));
    }

    @Override
    public void adviseBeforeReturnByThrow(long arg1, String arg2, int arg3, long arg4, int arg5) {
        super.adviseBeforeReturnByThrow(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), arg5);
    }

    @Override
    public void adviseAfterNew(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        super.adviseAfterNew(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6);
    }

    @Override
    public void adviseAfterNewArray(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, int arg7) {
        super.adviseAfterNewArray(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, arg7);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, String arg2, int arg3, float arg4) {
        super.adviseBeforeConstLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, String arg2, int arg3, double arg4) {
        super.adviseBeforeConstLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeConstLoad(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeConstLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeConstLoadObject(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeConstLoadObject(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeLoad(long arg1, String arg2, int arg3, int arg4) {
        super.adviseBeforeLoad(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeArrayLoad(long arg1, String arg2, int arg3, long arg4, int arg5) {
        super.adviseBeforeArrayLoad(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5);
    }

    @Override
    public void adviseBeforeStoreObject(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseBeforeStoreObject(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, float arg5) {
        super.adviseBeforeStore(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, double arg5) {
        super.adviseBeforeStore(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeStore(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseBeforeStore(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeArrayStoreObject(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        super.adviseBeforeArrayStoreObject(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, float arg6) {
        super.adviseBeforeArrayStore(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        super.adviseBeforeArrayStore(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeArrayStore(long arg1, String arg2, int arg3, long arg4, int arg5, double arg6) {
        super.adviseBeforeArrayStore(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseBeforeStackAdjust(long arg1, String arg2, int arg3, int arg4) {
        super.adviseBeforeStackAdjust(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, double arg5, double arg6) {
        super.adviseBeforeOperation(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, long arg5, long arg6) {
        super.adviseBeforeOperation(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeOperation(long arg1, String arg2, int arg3, int arg4, float arg5, float arg6) {
        super.adviseBeforeOperation(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6);
    }

    @Override
    public void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseBeforeConversion(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, float arg5) {
        super.adviseBeforeConversion(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeConversion(long arg1, String arg2, int arg3, int arg4, double arg5) {
        super.adviseBeforeConversion(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeIf(long arg1, String arg2, int arg3, int arg4, int arg5, int arg6, int arg7) {
        super.adviseBeforeIf(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public void adviseBeforeIfObject(long arg1, String arg2, int arg3, int arg4, long arg5, long arg6, int arg7) {
        super.adviseBeforeIfObject(arg1, getThreadShortForm(arg2), arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public void adviseBeforeGoto(long arg1, String arg2, int arg3, int arg4) {
        super.adviseBeforeGoto(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturnObject(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeReturnObject(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3, float arg4) {
        super.adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3, double arg4) {
        super.adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3, arg4);
    }

    @Override
    public void adviseBeforeReturn(long arg1, String arg2, int arg3) {
        super.adviseBeforeReturn(arg1, getThreadShortForm(arg2), arg3);
    }

    @Override
    public void adviseBeforeGetStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6) {
        super.adviseBeforeGetStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6));
    }

    @Override
    public void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, float arg7) {
        super.adviseBeforePutStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, double arg7) {
        super.adviseBeforePutStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforePutStatic(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, long arg7) {
        super.adviseBeforePutStatic(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforePutStaticObject(long arg1, String arg2, int arg3, String arg4, long arg5, String arg6, long arg7) {
        super.adviseBeforePutStaticObject(arg1, getThreadShortForm(arg2), arg3, getClassShortForm(arg4, arg5), arg5, getFieldShortForm(arg4, arg5, arg6), arg7);
    }

    @Override
    public void adviseBeforeGetField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeGetField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, float arg8) {
        super.adviseBeforePutField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, long arg8) {
        super.adviseBeforePutField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforePutField(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, double arg8) {
        super.adviseBeforePutField(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforePutFieldObject(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7, long arg8) {
        super.adviseBeforePutFieldObject(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getFieldShortForm(arg5, arg6, arg7), arg8);
    }

    @Override
    public void adviseBeforeInvokeVirtual(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeVirtual(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeInvokeSpecial(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeSpecial(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeInvokeStatic(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeStatic(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeInvokeInterface(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseBeforeInvokeInterface(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

    @Override
    public void adviseBeforeArrayLength(long arg1, String arg2, int arg3, long arg4, int arg5) {
        super.adviseBeforeArrayLength(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseBeforeThrow(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeThrow(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2));
    }

    @Override
    public void adviseBeforeCheckCast(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        super.adviseBeforeCheckCast(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6);
    }

    @Override
    public void adviseBeforeInstanceOf(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6) {
        super.adviseBeforeInstanceOf(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6);
    }

    @Override
    public void adviseBeforeMonitorEnter(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeMonitorEnter(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2));
    }

    @Override
    public void adviseBeforeMonitorExit(long arg1, String arg2, int arg3, long arg4) {
        super.adviseBeforeMonitorExit(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2));
    }

    @Override
    public void adviseAfterLoadObject(long arg1, String arg2, int arg3, int arg4, long arg5) {
        super.adviseAfterLoadObject(arg1, getThreadShortForm(arg2), arg3, arg4, arg5);
    }

    @Override
    public void adviseAfterArrayLoadObject(long arg1, String arg2, int arg3, long arg4, int arg5, long arg6) {
        super.adviseAfterArrayLoadObject(arg1, getThreadShortForm(arg2), arg3,  checkRepeatId(arg4, arg2), arg5, arg6);
    }

    @Override
    public void adviseAfterMethodEntry(long arg1, String arg2, int arg3, long arg4, String arg5, long arg6, String arg7) {
        super.adviseAfterMethodEntry(arg1, getThreadShortForm(arg2), arg3, checkRepeatId(arg4, arg2), getClassShortForm(arg5, arg6), arg6, getMethodShortForm(arg5, arg6, arg7));
    }

// END GENERATED CODE

}
