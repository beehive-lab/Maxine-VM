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
 * N.B. The classloader id associated with a class, cf. {@link ClassName} is only output when the
 * short form is defined.
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
            appendCode(INITIALIZE_LOG);
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
        appendCode(FINALIZE_LOG);
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

    private void appendCode(Key key) {
        sb.append(key.code);
    }

    private void appendTT(Key key, String threadName) {
        appendCode(key);
        appendSpace();
        appendTime();
        appendSpace();
        sb.append(threadName);
    }

    private void appendTTId(Key key, long objId, String threadName) {
        appendTT(key, threadName);
        appendSpace();
        appendId(objId);
        appendSpace();
    }

    private void appendTTIdIndex(Key key, long objId, String threadName, int index) {
        appendTT(key, threadName);
        appendSpace();
        appendId(objId);
        appendSpace();
        sb.append(index);
        appendSpace();
    }

    private void appendClassName(ClassName className) {
        sb.append(className.name);
    }

    private void appendQualName(QualName qualName) {
        appendClassName(qualName.className);
        appendSpace();
        sb.append(qualName.name);
    }

    private void appendTTC(Key key, String className, String threadName) {
        appendTT(key, threadName);
        appendSpace();
        sb.append(className);
        appendSpace();
    }

    private void appendPutFieldPrefix(long objId, QualName fieldName, String threadName) {
        appendTTId(ADVISE_BEFORE_PUT_FIELD, objId, threadName);
        appendQualName(fieldName);
        appendSpace();
    }

    private void appendPutStaticPrefix(QualName fieldName, String threadName) {
        appendTTC(ADVISE_BEFORE_PUT_STATIC, fieldName.className.name, threadName);
        sb.append(fieldName.name);
        appendSpace();
    }

    @Override
    public synchronized void adviseAfterGC(String threadName) {
        appendTT(ADVISE_AFTER_GC, threadName);
        end();
    }

    @Override
    public synchronized void removal(long id) {
        appendCode(REMOVAL);
        appendSpace();
        appendId(id);
        end();
    }

    @Override
    public synchronized void unseenObject(String threadName, long objId, ClassName className) {
        appendTTId(UNSEEN, objId, threadName);
        appendClassName(className);
        end();
    }

    @Override
    public synchronized void resetTime() {
        appendCode(RESET_TIME);
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
    public synchronized void adviseBeforeGetStatic(String threadName, QualName fieldName) {
        appendTTC(ADVISE_BEFORE_GET_STATIC, fieldName.className.name, threadName);
        sb.append(fieldName.name);
        appendSpace();
        sb.append(fieldName.className.clId);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, QualName fieldName, double value) {
        appendPutStaticPrefix(fieldName, threadName);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, QualName fieldName, long value) {
        appendPutStaticPrefix(fieldName, threadName);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStatic(String threadName, QualName fieldName, float value) {
        appendPutStaticPrefix(fieldName, threadName);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutStaticObject(String threadName, QualName fieldName, long valueId) {
        appendPutStaticPrefix(fieldName, threadName);
        sb.append(OBJ_VALUE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public synchronized void adviseBeforeGetField(String threadName, long objId, QualName fieldName) {
        appendTTId(ADVISE_BEFORE_GET_FIELD, objId, threadName);
        appendQualName(fieldName);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, QualName fieldName, double value) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, QualName fieldName, long value) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutField(String threadName, long objId, QualName fieldName, float value) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforePutFieldObject(String threadName, long objId, QualName fieldName, long valueId) {
        appendPutFieldPrefix(objId, fieldName, threadName);
        sb.append(OBJ_VALUE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayLoad(String threadName, long objId, int index) {
        appendTTIdIndex(ADVISE_BEFORE_ARRAY_LOAD, objId, threadName, index);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String threadName, long objId, int index, float value) {
        appendTTIdIndex(ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String threadName, long objId, int index, long value) {
        appendTTIdIndex(ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStore(String threadName, long objId, int index, double value) {
        appendTTIdIndex(ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayStoreObject(String threadName, long objId, int index, long valueId) {
        appendTTIdIndex(ADVISE_BEFORE_ARRAY_STORE, objId, threadName, index);
        sb.append(OBJ_VALUE);
        appendSpace();
        appendId(valueId);
        end();
    }

    @Override
    public synchronized void adviseAfterNew(String threadName, long objId, ClassName className) {
        appendTTId(ADVISE_AFTER_NEW, objId, threadName);
        appendClassName(className);
        end();
    }

    @Override
    public synchronized void adviseAfterNewArray(String threadName, long objId, ClassName className, int length) {
        appendTTId(ADVISE_AFTER_NEW_ARRAY, objId, threadName);
        appendClassName(className);
        appendSpace();
        sb.append(length);
        end();
    }

    @Override
    public synchronized void adviseAfterMultiNewArray(String threadName, long objId, ClassName className, int length) {
        // MultiArrays are explicitly handled by multiple calls to adviseAfterNewArray so we just
        // log the top level array.
        adviseAfterNewArray(threadName, objId, className, length);
    }

    @Override
    public synchronized void adviseBeforeGC(String threadName) {
        appendTT(ADVISE_BEFORE_GC, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String threadName, long value) {
        appendTT(ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoadObject(String threadName, long value) {
        appendTT(ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String threadName, float value) {
        appendTT(ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeConstLoad(String threadName, double value) {
        appendTT(ADVISE_BEFORE_CONST_LOAD, threadName);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeIPush(String threadName, int arg1) {
        appendTT(ADVISE_BEFORE_IPUSH, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public synchronized void adviseBeforeLoad(String threadName, int arg1) {
        appendTT(ADVISE_BEFORE_LOAD, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public synchronized void adviseBeforeStore(String threadName, int dispToLocalSlot, long value) {
        appendTT(ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStore(String threadName, int dispToLocalSlot, float value) {
        appendTT(ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStore(String threadName, int dispToLocalSlot, double value) {
        appendTT(ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStoreObject(String threadName, int dispToLocalSlot, long value) {
        appendTT(ADVISE_BEFORE_STORE, threadName);
        appendSpace();
        sb.append(dispToLocalSlot);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeStackAdjust(String threadName, int arg1) {
        appendTT(ADVISE_BEFORE_STACK_ADJUST, threadName);

        appendSpace();
        sb.append(arg1);
        end();
    }

    private void prefixAdviseBeforeOperation(String threadName, int arg1) {
        appendTT(ADVISE_BEFORE_OPERATION, threadName);
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
        appendTT(ADVISE_BEFORE_IINC, threadName);
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
        appendTT(ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeConversion(String threadName, int arg1, float arg2) {
        appendTT(ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeConversion(String threadName, int arg1, double arg2) {
        appendTT(ADVISE_BEFORE_CONVERSION, threadName);
        appendSpace();
        sb.append(arg1);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(arg2);
        appendSpace();
        end();
    }

    @Override
    public synchronized void adviseBeforeIf(String threadName, int opcode, int op1, int op2) {
        appendTT(ADVISE_BEFORE_IF, threadName);
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
        appendTT(ADVISE_BEFORE_IF, threadName);
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
        appendTT(ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(OBJ_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName, long value) {
        appendTT(ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(LONG_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName, float value) {
        appendTT(ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(FLOAT_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName, double value) {
        appendTT(ADVISE_BEFORE_RETURN, threadName);
        appendSpace();
        sb.append(DOUBLE_VALUE);
        appendSpace();
        sb.append(value);
        end();
    }

    @Override
    public synchronized void adviseBeforeReturn(String threadName) {
        appendTT(ADVISE_BEFORE_RETURN, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeVirtual(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_BEFORE_INVOKE_VIRTUAL, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeSpecial(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_BEFORE_INVOKE_SPECIAL, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeStatic(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_BEFORE_INVOKE_STATIC, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeInvokeInterface(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_BEFORE_INVOKE_INTERFACE, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseBeforeArrayLength(String threadName, long objId, int length) {
        appendTTId(ADVISE_BEFORE_ARRAY_LENGTH, objId, threadName);
        sb.append(length);
        end();
    }

    @Override
    public synchronized void adviseBeforeThrow(String threadName, long objId) {
        appendTTId(ADVISE_BEFORE_THROW, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeCheckCast(String threadName, long objId, ClassName className) {
        appendTTId(ADVISE_BEFORE_CHECK_CAST, objId, threadName);
        appendClassName(className);
        end();
    }

    @Override
    public synchronized void adviseBeforeInstanceOf(String threadName, long objId, ClassName className) {
        appendTTId(ADVISE_BEFORE_INSTANCE_OF, objId, threadName);
        appendClassName(className);
        end();
    }

    @Override
    public synchronized void adviseBeforeMonitorEnter(String threadName, long objId) {
        appendTTId(ADVISE_BEFORE_MONITOR_ENTER, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeMonitorExit(String threadName, long objId) {
        appendTTId(ADVISE_BEFORE_MONITOR_EXIT, objId, threadName);
        end();
    }

    @Override
    public synchronized void adviseBeforeBytecode(String threadName, int arg1) {
        appendTT(ADVISE_BEFORE_BYTECODE, threadName);
        appendSpace();
        sb.append(arg1);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeVirtual(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_AFTER_INVOKE_VIRTUAL, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeStatic(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_AFTER_INVOKE_STATIC, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeInterface(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_AFTER_INVOKE_INTERFACE, objId, threadName);
        appendQualName(methodName);
        end();
    }

    @Override
    public synchronized void adviseAfterInvokeSpecial(String threadName, long objId, QualName methodName) {
        appendTTId(ADVISE_AFTER_INVOKE_SPECIAL, objId, threadName);
        appendQualName(methodName);
        end();
    }


}
