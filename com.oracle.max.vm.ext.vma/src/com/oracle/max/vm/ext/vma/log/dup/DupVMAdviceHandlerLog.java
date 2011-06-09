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
package com.oracle.max.vm.ext.vma.log.dup;

import com.oracle.max.vm.ext.vma.log.*;


/**
 * A debugging tool really; allows the log to be sent to two delegate loggers for later (semantic) comparison.
 * The {@value DUPCLASSES_PROPERTY} should be set to XX,YY, which will result in the
 * attempted instantiation of the two delegate loggers, {@code com.oracle.max.vm.ext.vma.log.XXVMAdviceHandlerLog}
 * and {@code com.oracle.max.vm.ext.vma.log.YYVMAdviceHandlerLog}.
 *
 */
public class DupVMAdviceHandlerLog extends VMAdviceHandlerLog {
    private static final String DUPCLASSES_PROPERTY = "max.vma.duplog";
    private VMAdviceHandlerLog a;
    private VMAdviceHandlerLog b;

    @Override
    public boolean initializeLog() {
        final String dupLogProp = System.getProperty(DUPCLASSES_PROPERTY);
        if (dupLogProp == null) {
            System.err.println(DUPCLASSES_PROPERTY + " not set");
            return false;
        }
        final String[] pair = dupLogProp.split(",");
        a = create(pair[0]);
        b = create(pair[1]);
        return true;
    }

    private VMAdviceHandlerLog create(String id) {
        // Need to mess with the log file/log class properties to duplicate it.
        final String logFile = VMAdviceHandlerLogFile.getLogFile();
        System.setProperty(VMAdviceHandlerLogFile.LOGFILE_PROPERTY, logFile + "-" + id);
        System.setProperty(VMAdviceHandlerLogFactory.LOGCLASS_PROPERTY, "com.oracle.max.vm.ext.vma.log." + id + "VMAdviceHandlerLog");
        final VMAdviceHandlerLog result = VMAdviceHandlerLogFactory.create();
        result.initializeLog();
        System.setProperty(VMAdviceHandlerLogFile.LOGFILE_PROPERTY, logFile);
        return result;
    }

    @Override
    public void finalizeLog() {
        a.finalizeLog();
        b.finalizeLog();
    }

    @Override
    public void removal(long id) {
        a.removal(id);
        b.removal(id);
    }

    @Override
    public void unseenObject(long objId,
            String className, long clId) {
        a.unseenObject(objId, className, clId);
        b.unseenObject(objId, className, clId);
    }

    @Override
    public void resetTime() {
        a.resetTime();
        b.resetTime();
    }


    // BEGIN GENERATED CODE

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGC(String arg1) {
        a.adviseBeforeGC(arg1);
        b.adviseBeforeGC(arg1);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterGC(String arg1) {
        a.adviseAfterGC(arg1);
        b.adviseAfterGC(arg1);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThreadStarting(String arg1) {
        a.adviseBeforeThreadStarting(arg1);
        b.adviseBeforeThreadStarting(arg1);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThreadTerminating(String arg1) {
        a.adviseBeforeThreadTerminating(arg1);
        b.adviseBeforeThreadTerminating(arg1);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, long arg2) {
        a.adviseBeforeConstLoad(arg1, arg2);
        b.adviseBeforeConstLoad(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, float arg2) {
        a.adviseBeforeConstLoad(arg1, arg2);
        b.adviseBeforeConstLoad(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoad(String arg1, double arg2) {
        a.adviseBeforeConstLoad(arg1, arg2);
        b.adviseBeforeConstLoad(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConstLoadObject(String arg1, long arg2) {
        a.adviseBeforeConstLoadObject(arg1, arg2);
        b.adviseBeforeConstLoadObject(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIPush(String arg1, int arg2) {
        a.adviseBeforeIPush(arg1, arg2);
        b.adviseBeforeIPush(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeLoad(String arg1, int arg2) {
        a.adviseBeforeLoad(arg1, arg2);
        b.adviseBeforeLoad(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLoad(String arg1, long arg2, int arg3) {
        a.adviseBeforeArrayLoad(arg1, arg2, arg3);
        b.adviseBeforeArrayLoad(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, long arg3) {
        a.adviseBeforeStore(arg1, arg2, arg3);
        b.adviseBeforeStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, float arg3) {
        a.adviseBeforeStore(arg1, arg2, arg3);
        b.adviseBeforeStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStore(String arg1, int arg2, double arg3) {
        a.adviseBeforeStore(arg1, arg2, arg3);
        b.adviseBeforeStore(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStoreObject(String arg1, int arg2, long arg3) {
        a.adviseBeforeStoreObject(arg1, arg2, arg3);
        b.adviseBeforeStoreObject(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, float arg4) {
        a.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        b.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, long arg4) {
        a.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        b.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStore(String arg1, long arg2, int arg3, double arg4) {
        a.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
        b.adviseBeforeArrayStore(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayStoreObject(String arg1, long arg2, int arg3, long arg4) {
        a.adviseBeforeArrayStoreObject(arg1, arg2, arg3, arg4);
        b.adviseBeforeArrayStoreObject(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeStackAdjust(String arg1, int arg2) {
        a.adviseBeforeStackAdjust(arg1, arg2);
        b.adviseBeforeStackAdjust(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, long arg3, long arg4) {
        a.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        b.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, float arg3, float arg4) {
        a.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        b.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeOperation(String arg1, int arg2, double arg3, double arg4) {
        a.adviseBeforeOperation(arg1, arg2, arg3, arg4);
        b.adviseBeforeOperation(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIInc(String arg1, int arg2, int arg3, int arg4) {
        a.adviseBeforeIInc(arg1, arg2, arg3, arg4);
        b.adviseBeforeIInc(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, long arg3) {
        a.adviseBeforeConversion(arg1, arg2, arg3);
        b.adviseBeforeConversion(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, float arg3) {
        a.adviseBeforeConversion(arg1, arg2, arg3);
        b.adviseBeforeConversion(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeConversion(String arg1, int arg2, double arg3) {
        a.adviseBeforeConversion(arg1, arg2, arg3);
        b.adviseBeforeConversion(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIf(String arg1, int arg2, int arg3, int arg4) {
        a.adviseBeforeIf(arg1, arg2, arg3, arg4);
        b.adviseBeforeIf(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeIfObject(String arg1, int arg2, long arg3, long arg4) {
        a.adviseBeforeIfObject(arg1, arg2, arg3, arg4);
        b.adviseBeforeIfObject(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturnObject(String arg1, long arg2) {
        a.adviseBeforeReturnObject(arg1, arg2);
        b.adviseBeforeReturnObject(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, long arg2) {
        a.adviseBeforeReturn(arg1, arg2);
        b.adviseBeforeReturn(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, float arg2) {
        a.adviseBeforeReturn(arg1, arg2);
        b.adviseBeforeReturn(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1, double arg2) {
        a.adviseBeforeReturn(arg1, arg2);
        b.adviseBeforeReturn(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeReturn(String arg1) {
        a.adviseBeforeReturn(arg1);
        b.adviseBeforeReturn(arg1);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetStatic(String arg1, String arg2, long arg3, String arg4) {
        a.adviseBeforeGetStatic(arg1, arg2, arg3, arg4);
        b.adviseBeforeGetStatic(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, double arg5) {
        a.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        b.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, long arg5) {
        a.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        b.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStatic(String arg1, String arg2, long arg3, String arg4, float arg5) {
        a.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
        b.adviseBeforePutStatic(arg1, arg2, arg3, arg4, arg5);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutStaticObject(String arg1, String arg2, long arg3, String arg4, long arg5) {
        a.adviseBeforePutStaticObject(arg1, arg2, arg3, arg4, arg5);
        b.adviseBeforePutStaticObject(arg1, arg2, arg3, arg4, arg5);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeGetField(String arg1, long arg2, String arg3) {
        a.adviseBeforeGetField(arg1, arg2, arg3);
        b.adviseBeforeGetField(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, String arg3, double arg4) {
        a.adviseBeforePutField(arg1, arg2, arg3, arg4);
        b.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, String arg3, long arg4) {
        a.adviseBeforePutField(arg1, arg2, arg3, arg4);
        b.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutField(String arg1, long arg2, String arg3, float arg4) {
        a.adviseBeforePutField(arg1, arg2, arg3, arg4);
        b.adviseBeforePutField(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforePutFieldObject(String arg1, long arg2, String arg3, long arg4) {
        a.adviseBeforePutFieldObject(arg1, arg2, arg3, arg4);
        b.adviseBeforePutFieldObject(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeVirtual(String arg1, long arg2, String arg3) {
        a.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
        b.adviseBeforeInvokeVirtual(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeSpecial(String arg1, long arg2, String arg3) {
        a.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
        b.adviseBeforeInvokeSpecial(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeStatic(String arg1, long arg2, String arg3) {
        a.adviseBeforeInvokeStatic(arg1, arg2, arg3);
        b.adviseBeforeInvokeStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInvokeInterface(String arg1, long arg2, String arg3) {
        a.adviseBeforeInvokeInterface(arg1, arg2, arg3);
        b.adviseBeforeInvokeInterface(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeArrayLength(String arg1, long arg2, int arg3) {
        a.adviseBeforeArrayLength(arg1, arg2, arg3);
        b.adviseBeforeArrayLength(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeThrow(String arg1, long arg2) {
        a.adviseBeforeThrow(arg1, arg2);
        b.adviseBeforeThrow(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeCheckCast(String arg1, long arg2, String arg3, long arg4) {
        a.adviseBeforeCheckCast(arg1, arg2, arg3, arg4);
        b.adviseBeforeCheckCast(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeInstanceOf(String arg1, long arg2, String arg3, long arg4) {
        a.adviseBeforeInstanceOf(arg1, arg2, arg3, arg4);
        b.adviseBeforeInstanceOf(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorEnter(String arg1, long arg2) {
        a.adviseBeforeMonitorEnter(arg1, arg2);
        b.adviseBeforeMonitorEnter(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeMonitorExit(String arg1, long arg2) {
        a.adviseBeforeMonitorExit(arg1, arg2);
        b.adviseBeforeMonitorExit(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseBeforeBytecode(String arg1, int arg2) {
        a.adviseBeforeBytecode(arg1, arg2);
        b.adviseBeforeBytecode(arg1, arg2);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeVirtual(String arg1, long arg2, String arg3) {
        a.adviseAfterInvokeVirtual(arg1, arg2, arg3);
        b.adviseAfterInvokeVirtual(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeSpecial(String arg1, long arg2, String arg3) {
        a.adviseAfterInvokeSpecial(arg1, arg2, arg3);
        b.adviseAfterInvokeSpecial(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeStatic(String arg1, long arg2, String arg3) {
        a.adviseAfterInvokeStatic(arg1, arg2, arg3);
        b.adviseAfterInvokeStatic(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterInvokeInterface(String arg1, long arg2, String arg3) {
        a.adviseAfterInvokeInterface(arg1, arg2, arg3);
        b.adviseAfterInvokeInterface(arg1, arg2, arg3);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNew(String arg1, long arg2, String arg3, long arg4) {
        a.adviseAfterNew(arg1, arg2, arg3, arg4);
        b.adviseAfterNew(arg1, arg2, arg3, arg4);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterNewArray(String arg1, long arg2, String arg3, long arg4, int arg5) {
        a.adviseAfterNewArray(arg1, arg2, arg3, arg4, arg5);
        b.adviseAfterNewArray(arg1, arg2, arg3, arg4, arg5);
    }

    // GENERATED -- EDIT AND RUN DupVMAdviceHandlerLogGenerator.main() TO MODIFY
    @Override
    public void adviseAfterMultiNewArray(String arg1, long arg2, String arg3, long arg4, int arg5) {
        a.adviseAfterMultiNewArray(arg1, arg2, arg3, arg4, arg5);
        b.adviseAfterMultiNewArray(arg1, arg2, arg3, arg4, arg5);
    }


}
