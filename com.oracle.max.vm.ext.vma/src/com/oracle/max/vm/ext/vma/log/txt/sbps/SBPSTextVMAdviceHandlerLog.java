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
package com.oracle.max.vm.ext.vma.log.txt.sbps;

import java.io.*;

import static com.oracle.max.vm.ext.vma.log.txt.TextVMAdviceHandlerLog.Key.*;
import com.oracle.max.vm.ext.vma.log.*;
import com.oracle.max.vm.ext.vma.log.txt.*;

/**
 * An implementation of {@link TextVMAdviceHandlerLog} in Java using a {@link PrintStream} and {@link StringBuilder}.
 *
 * The default {@link StringBuilder buffer size} is {@value DEFAULT_BUFSIZE} but this can be changed
 * with the {@value BUFSIZE_PROPERTY} system property. The buffer is normally flushed when it is full,
 * but this can be changed to a specific value by setting the {@value FLUSH_PROPERTY} system property.
 *
 * Since the buffer is global to all threads all entry methods are synchronized.
 *
 */
public class SBPSTextVMAdviceHandlerLog extends TextVMAdviceHandlerLog {

    private static final String FLUSH_PROPERTY = "max.vma.flushlogat";
    private static final String BUFSIZE_PROPERTY = "max.vma.logbufsize";
    private static final String ABSTIME_PROPERTY = "max.vma.abstime";
    private static final int DEFAULT_BUFSIZE = 1024 * 1024;
    private PrintStream ps;
    private long lastTime;
    private boolean absTime;
    StringBuilder sb;
    private int flushLogAt;

    public SBPSTextVMAdviceHandlerLog() {
        super();
    }

    @Override
    public boolean initializeLog() {
        final String logFile = VMAdviceHandlerLogFile.getLogFile();
        try {
            ps = new PrintStream(new FileOutputStream(logFile));
            lastTime = timeStampGenerator.getTimeStamp();
            int logSize = DEFAULT_BUFSIZE;
            final String logSizeProp = System.getProperty(BUFSIZE_PROPERTY);
            if (logSizeProp != null) {
                logSize = Integer.parseInt(logSizeProp);
            }
            sb = new StringBuilder(logSize);
            flushLogAt = System.getProperty(FLUSH_PROPERTY) != null ? 0 : logSize - 80;
            absTime = System.getProperty(ABSTIME_PROPERTY) != null;
            sb.append(INITIALIZE_LOG.code);
            appendSpace();
            sb.append(lastTime);
            appendSpace();
            sb.append(absTime);
            end();
            return true;
        } catch (IOException ex) {
            System.err.println("failed to open log file " + logFile + ": " + ex);
            return false;
        }
    }

    @Override
    public synchronized void finalizeLog() {
        sb.append(FINALIZE_LOG.code);
        appendSpace();
        sb.append(timeStampGenerator.getTimeStamp());
        flushLogAt = 0;
        end();
        ps.close();
    }

    void end() {
        sb.append('\n');
        if (sb.length()  >= flushLogAt) {
            ps.print(sb);
            ps.flush();
            sb.setLength(0);
        }
    }

    private void appendId(long objId) {
        if (objId == REPEAT_ID_VALUE) {
            sb.append('*');
        } else {
            sb.append(objId);
        }
    }

    private void appendTime() {
        final long now = timeStampGenerator.getTimeStamp();
        if (absTime) {
            sb.append(now);
        } else {
            sb.append(now - lastTime);
            lastTime = now;
        }
    }

    private void appendSpace() {
        sb.append(' ');
    }

    private void appendTT(StringBuilder sb, Key key, String threadName) {
        sb.append(key.code);
        appendSpace();
        appendTime();
        appendSpace();
        sb.append(threadName);
    }

    private void appendTTId(StringBuilder sb, Key key, long objId, String threadName) {
        appendTT(sb, key, threadName);
        appendSpace();
        appendId(objId);
        appendSpace();
    }

    private void appendTTC(StringBuilder sb, Key key, String className, String threadName) {
        appendTT(sb, key, threadName);
        appendSpace();
        sb.append(className);
        appendSpace();
    }

    private void appendPutFieldPrefix(long objId, String fieldName, String threadName) {
        appendTTId(sb, ADVISE_BEFORE_PUT_FIELD, objId, threadName);
        sb.append(fieldName);
        appendSpace();
    }

    private void appendPutStaticPrefix(String fieldName, String className, long clId, String threadName) {
        appendTTC(sb, ADVISE_BEFORE_PUT_STATIC, className, threadName);
        sb.append(clId);
        appendSpace();
        sb.append(fieldName);
        appendSpace();
    }

    @Override
    public synchronized void adviseAfterGC(String threadName) {
        appendTT(sb, ADVISE_AFTER_GC, threadName);
        end();
    }

    @Override
    public synchronized void removal(long id) {
        sb.append(REMOVAL);
        appendSpace();
        appendId(id);
        end();
    }

    @Override
    public synchronized void unseenObject(long objId,
            String className, long clId) {
        sb.append(UNSEEN.code);
        appendSpace();
        appendTime();
        appendSpace();
        appendId(objId);
        appendSpace();
        sb.append(className);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void resetTime() {
        sb.append(RESET_TIME.code);
        appendSpace();
        lastTime = timeStampGenerator.getTimeStamp();
        sb.append(lastTime);
        end();
    }

    @Override
    public void adviseBeforeThreadStarting(String threadName) {
    }

    @Override
    public void adviseBeforeThreadTerminating(String threadName) {
    }

    @Override
    public synchronized void adviseBeforeGetStatic(String threadName, String className, long clId, String fieldName) {
        appendTTC(sb, ADVISE_BEFORE_GET_STATIC, className, threadName);
        sb.append(fieldName);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, double value) {
        appendPutStaticPrefix(fieldName, className, clId, threadName);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, long value) {
        appendPutStaticPrefix(fieldName, className, clId, threadName);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, String className, long clId, String fieldName, float value) {
        appendPutStaticPrefix(fieldName, className, clId, threadName);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStaticObject(String threadName, String className, long clId, String fieldName, long valueId) {
        appendPutStaticPrefix(fieldName, className, clId, threadName);
        sb.append(OBJ_VALUE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public synchronized void adviseBeforeGetField(String threadName, long objId, String fieldName) {
        appendTTId(sb, ADVISE_BEFORE_GET_FIELD, objId, threadName);
        sb.append(fieldName);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, String fieldName, double value) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, String fieldName, long value) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, String fieldName, float value) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutFieldObject(String threadName, long objId, String fieldName, long valueId) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(OBJ_VALUE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayLoad(String threadName, long objId, int index) {
        appendTTId(sb, ADVISE_BEFORE_ARRAY_LOAD, objId, threadName);
        sb.append(index);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String threadName, long objId, int index, float value) {
        appendTTId(sb, ADVISE_BEFORE_ARRAY_STORE, objId, threadName);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String threadName, long objId, int index, long value) {
        appendTTId(sb, ADVISE_BEFORE_ARRAY_STORE, objId, threadName);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String threadName, long objId, int index, double value) {
        appendTTId(sb, ADVISE_BEFORE_ARRAY_STORE, objId, threadName);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStoreObject(String threadName, long objId, int index, long valueId) {
        appendTTId(sb, ADVISE_BEFORE_ARRAY_STORE, objId, threadName);
        sb.append(OBJ_VALUE);
        appendSpace();
        appendId(valueId);
        appendSpace();
        sb.append(index);
        end();
    }

    @Override
    public synchronized void adviseAfterNew(String threadName, long objId, String className, long clId) {
        appendTTId(sb, ADVISE_AFTER_NEW, objId, threadName);
        sb.append(className);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void adviseAfterNewArray(String threadName, long objId, String className, long clId, int length) {
        appendTTId(sb, ADVISE_AFTER_NEW_ARRAY, objId, threadName);
        sb.append(className);
        appendSpace();
        sb.append(clId);
        appendSpace();
        sb.append(length);
        end();
    }

    @Override
    public synchronized void adviseAfterMultiNewArray(String threadName, long objId, String className, long clId, int length) {
        // MultiArrays are explicitly handled by multiple calls to adviseAfterNewArray so we just
        // log the top level array.
        adviseAfterNewArray(threadName, objId, className, clId, length);
    }

    @Override
    public synchronized void adviseBeforeGC(String threadName) {
        appendTT(sb, ADVISE_BEFORE_GC, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String threadName, long value) {
        appendTT(sb, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoadObject(String threadName, long value) {
        appendTT(sb, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String threadName, float value) {
        appendTT(sb, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String threadName, double value) {
        appendTT(sb, ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeIPush(String threadName, int arg1) {
        appendTT(sb, ADVISE_BEFORE_IPUSH, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public synchronized void adviseBeforeLoad(String threadName, int arg1) {
        appendTT(sb, ADVISE_BEFORE_LOAD, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public synchronized void adviseBeforeStore(String threadName, int dispToLocalSlot, long value) {
        appendTT(sb, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStore(String threadName, int dispToLocalSlot, float value) {
        appendTT(sb, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStore(String threadName, int dispToLocalSlot, double value) {
        appendTT(sb, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStoreObject(String threadName, int dispToLocalSlot, long value) {
        appendTT(sb, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStackAdjust(String threadName, int arg1) {
        appendTT(sb, ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    private void prefixAdviseBeforeOperation(String threadName, int arg1) {
        appendTT(sb, ADVISE_BEFORE_OPERATION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
    }

    @Override
    public synchronized void adviseBeforeOperation(String threadName, int arg1, long arg2, long arg3) {
        prefixAdviseBeforeOperation(threadName, arg1);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public synchronized void adviseBeforeOperation(String threadName, int arg1, float arg2, float arg3) {
        prefixAdviseBeforeOperation(threadName, arg1);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public synchronized void adviseBeforeOperation(String threadName, int arg1, double arg2, double arg3) {
        prefixAdviseBeforeOperation(threadName, arg1);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public synchronized void adviseBeforeIInc(String threadName, int arg1, int arg2, int arg3) {
        appendTT(sb, ADVISE_BEFORE_IINC, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        sb.append(arg3);
        end();
    }

    @Override
    public synchronized void adviseBeforeConversion(String threadName, int arg1, long arg2) {
        appendTT(sb, ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeConversion(String threadName, int arg1, float arg2) {
        appendTT(sb, ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeConversion(String threadName, int arg1, double arg2) {
        appendTT(sb, ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeIf(String threadName, int opcode, int op1, int op2) {
        appendTT(sb, ADVISE_BEFORE_IF, threadName);
        appendSpace();
        sb.append(opcode);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(op1);
        appendSpace();
        sb.append(op2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeIfObject(String threadName, int opcode, long objId1, long objId2) {
        appendTT(sb, ADVISE_BEFORE_IF, threadName);
        appendSpace();
        sb.append(opcode);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(objId1);
        appendSpace();
        sb.append(objId2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeReturnObject(String threadName, long value) {
        appendTT(sb, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName, long value) {
        appendTT(sb, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName, float value) {
        appendTT(sb, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName, double value) {
        appendTT(sb, ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName) {
        appendTT(sb, ADVISE_BEFORE_RETURN, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeVirtual(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_BEFORE_INVOKE_VIRTUAL, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeSpecial(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_BEFORE_INVOKE_SPECIAL, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeStatic(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_BEFORE_INVOKE_STATIC, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeInterface(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_BEFORE_INVOKE_INTERFACE, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayLength(String threadName, long objId, int length) {
        appendTTId(sb, ADVISE_BEFORE_ARRAY_LENGTH, objId, threadName);
        appendSpace();
        sb.append(length);
        end();
    }

    @Override
    public synchronized void adviseBeforeThrow(String threadName, long objId) {
        appendTTId(sb, ADVISE_BEFORE_THROW, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeCheckCast(String threadName, long objId, String className, long clId) {
        appendTTId(sb, ADVISE_BEFORE_CHECK_CAST, objId, threadName);
        appendSpace();
        sb.append(className);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void adviseBeforeInstanceOf(String threadName, long objId, String className, long clId) {
        appendTTId(sb, ADVISE_BEFORE_INSTANCE_OF, objId, threadName);
        appendSpace();
        sb.append(className);
        appendSpace();
        sb.append(clId);
        end();
    }

    @Override
    public synchronized void adviseBeforeMonitorEnter(String threadName, long objId) {
        appendTTId(sb, ADVISE_BEFORE_MONITOR_ENTER, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeMonitorExit(String threadName, long objId) {
        appendTTId(sb, ADVISE_BEFORE_MONITOR_EXIT, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeBytecode(String threadName, int arg1) {
        appendTT(sb, ADVISE_BEFORE_BYTECODE, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeVirtual(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_AFTER_INVOKE_VIRTUAL, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeStatic(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_AFTER_INVOKE_STATIC, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeInterface(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_AFTER_INVOKE_INTERFACE, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeSpecial(String threadName, long objId, String methodName) {
        appendTTId(sb, ADVISE_AFTER_INVOKE_SPECIAL, objId, threadName);
        appendSpace();
        sb.append(methodName);
        end();
    }


}
